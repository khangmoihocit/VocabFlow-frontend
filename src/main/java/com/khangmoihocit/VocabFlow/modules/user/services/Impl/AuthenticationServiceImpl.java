package com.khangmoihocit.VocabFlow.modules.user.services.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khangmoihocit.VocabFlow.core.enums.ErrorCode;
import com.khangmoihocit.VocabFlow.core.exception.AppException;
import com.khangmoihocit.VocabFlow.core.exception.ValidTokenException;
import com.khangmoihocit.VocabFlow.core.security.JwtService;
import com.khangmoihocit.VocabFlow.core.security.UserDetailsCustom;
import com.khangmoihocit.VocabFlow.modules.user.dtos.request.AuthenticationRequest;
import com.khangmoihocit.VocabFlow.modules.user.dtos.request.RefreshTokenRequest;
import com.khangmoihocit.VocabFlow.modules.user.dtos.request.UserCreationRequest;
import com.khangmoihocit.VocabFlow.modules.user.dtos.response.AuthenticationResponse;
import com.khangmoihocit.VocabFlow.modules.user.dtos.response.UserResponse;
import com.khangmoihocit.VocabFlow.modules.user.entities.RefreshToken;
import com.khangmoihocit.VocabFlow.modules.user.entities.User;
import com.khangmoihocit.VocabFlow.modules.user.mappers.UserMapper;
import com.khangmoihocit.VocabFlow.modules.user.repositories.RefreshTokenRepository;
import com.khangmoihocit.VocabFlow.modules.user.repositories.UserRepository;
import com.khangmoihocit.VocabFlow.modules.user.services.AuthenticationService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {
    JwtService jwtService;
    AuthenticationManager authenticationManager;
    UserDetailsServiceImpl userDetailsService;
    RefreshTokenRepository refreshTokenRepository;
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Override
    public AuthenticationResponse authentication(AuthenticationRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsCustom userDetails = (UserDetailsCustom) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(userDetails.getUsername());
        String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername());

        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<RefreshToken> refreshTokenList = refreshTokenRepository.getAllByUserId(user.getId());

        //nếu user này có 2 token rồi, sẽ cập nhật lại token cũ nhất và đăng xuất khỏi thiết bị đó
        if (refreshTokenList.size() > 1) {
            RefreshToken refreshTokenSave = refreshTokenList.get(0);
            refreshTokenSave.setToken(refreshToken);
            refreshTokenSave.setExpiryDate(jwtService.extractExpired(refreshToken));
            refreshTokenRepository.save(refreshTokenSave);
        } else {
            //user mới đăng nhập 1 tb sẽ tạo mới refreshtoken
            refreshTokenRepository.save(RefreshToken.builder()
                    .token(refreshToken)
                    .user(user)
                    .expiryDate(jwtService.extractExpired(refreshToken))
                    .build());
        }
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .role(user.getRole())
                        .email(user.getEmail())
                        .isActive(user.getIsActive())
                        .build())
                .build();
    }

    @Override
    public UserResponse register(UserCreationRequest request) {
        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        try {
            user = userRepository.save(user);

        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        return userMapper.toUserResponse(user);
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        try{
            final String username = jwtService.extractUsername(refreshTokenRequest.getRefreshToken());
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return null;
        }catch (ExpiredJwtException ex) {
            throw new ValidTokenException("Token đã hết hạn, vui lòng đăng nhập lại");
        }
    }


}
