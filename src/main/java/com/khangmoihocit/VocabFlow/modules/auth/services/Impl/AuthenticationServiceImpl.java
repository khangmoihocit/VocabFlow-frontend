package com.khangmoihocit.VocabFlow.modules.auth.services.Impl;

import com.khangmoihocit.VocabFlow.core.enums.ErrorCode;
import com.khangmoihocit.VocabFlow.core.exception.AppException;
import com.khangmoihocit.VocabFlow.core.utils.JwtUtil;
import com.khangmoihocit.VocabFlow.modules.auth.dtos.UserDetailsCustom;
import com.khangmoihocit.VocabFlow.modules.auth.dtos.request.AuthenticationRequest;
import com.khangmoihocit.VocabFlow.modules.auth.dtos.response.AuthenticationResponse;
import com.khangmoihocit.VocabFlow.modules.auth.dtos.response.UserResponse;
import com.khangmoihocit.VocabFlow.modules.auth.entities.RefreshToken;
import com.khangmoihocit.VocabFlow.modules.auth.entities.User;
import com.khangmoihocit.VocabFlow.modules.auth.repositories.RefreshTokenRepository;
import com.khangmoihocit.VocabFlow.modules.auth.repositories.UserRepository;
import com.khangmoihocit.VocabFlow.modules.auth.services.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {
    JwtUtil jwtUtil;
    AuthenticationManager authenticationManager;
    UserDetailsServiceImpl userDetailsService;
    RefreshTokenRepository refreshTokenRepository;
    UserRepository userRepository;

    @Override
    public AuthenticationResponse authentication(AuthenticationRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsCustom userDetails = (UserDetailsCustom) authentication.getPrincipal();

        String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiryDate(jwtUtil.extractExpired(refreshToken))
                .build());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponse.builder()
                        .id(user.getId().toString())
                        .fullName(user.getFullName())
                        .role(user.getRole())
                        .email(user.getEmail())
                        .isActive(user.getIsActive())
                        .build())
                .build();
    }
}
