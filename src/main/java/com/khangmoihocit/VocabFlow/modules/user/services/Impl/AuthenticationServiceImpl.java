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
import com.khangmoihocit.VocabFlow.modules.user.dtos.response.RefreshTokenResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j(topic = "AUTHENTICATION SERVICE")
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
        saveRefreshTokenToDB(refreshToken, user);

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

    private void saveRefreshTokenToDB(String refreshToken, User user) {
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
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        try {
            final String username = jwtService.extractUsername(refreshTokenRequest.getRefreshToken());
            UserDetailsCustom userDetails = (UserDetailsCustom) userDetailsService.loadUserByUsername(username);
            if (!username.equals(userDetails.getUsername())) {
                throw new ValidTokenException("User token không chính xác");
            }

            if (!userDetails.isEnabled()) {
                throw new ValidTokenException("Tài khoản của bạn hiện đang bị khóa");
            }

            if (!refreshTokenRepository.existsByToken(refreshTokenRequest.getRefreshToken())) {
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            refreshTokenRepository.deleteByToken(refreshTokenRequest.getRefreshToken());
            String newAccessToken = jwtService.generateAccessToken(username);
            String newRefreshToken = jwtService.generateRefreshToken(username);
            User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            saveRefreshTokenToDB(newRefreshToken, user);

            return RefreshTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken).build();
        } catch (ExpiredJwtException ex) {
            throw new ValidTokenException("Token đã hết hạn, vui lòng đăng nhập lại");
        } catch (SignatureException ex) {
            throw new ValidTokenException("Chữ ký token không hợp lệ hoặc đã bị giả mạo");
        } catch (MalformedJwtException ex) {
            throw new ValidTokenException("Token không đúng định dạng");
        } catch (UnsupportedJwtException ex) {
            throw new ValidTokenException("Định dạng Token không được hệ thống hỗ trợ");
        } catch (IllegalArgumentException ex) {
            throw new ValidTokenException("Token không hợp lệ hoặc bị trống");
        } catch (ValidTokenException | AppException ex) {
            throw new ValidTokenException(ex.getMessage());
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new ValidTokenException("Đã xảy ra lỗi trong quá trình xác thực token");
        }
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.deleteByToken(request.getRefreshToken());
    }


}
