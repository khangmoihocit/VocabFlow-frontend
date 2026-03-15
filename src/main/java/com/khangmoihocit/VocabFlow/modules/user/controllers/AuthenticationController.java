package com.khangmoihocit.VocabFlow.modules.user.controllers;

import com.khangmoihocit.VocabFlow.core.response.ApiResponse;
import com.khangmoihocit.VocabFlow.modules.user.dtos.request.AuthenticationRequest;
import com.khangmoihocit.VocabFlow.modules.user.dtos.request.RefreshTokenRequest;
import com.khangmoihocit.VocabFlow.modules.user.dtos.request.UserCreationRequest;
import com.khangmoihocit.VocabFlow.modules.user.dtos.response.AuthenticationResponse;
import com.khangmoihocit.VocabFlow.modules.user.dtos.response.UserResponse;
import com.khangmoihocit.VocabFlow.modules.user.services.AuthenticationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j(topic = "AuthenticationController")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
@RequestMapping("${spring.api.prefix}/auth")
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    ResponseEntity<?> login(@Valid @RequestBody AuthenticationRequest request){
        AuthenticationResponse authenticationResponse = authenticationService.authentication(request);
        ApiResponse<AuthenticationResponse> response =
                ApiResponse.success(authenticationResponse, "Đăng nhập thành công!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    ResponseEntity<?> register(@Valid @RequestBody UserCreationRequest request){
        ApiResponse<UserResponse> response =
                ApiResponse.success(authenticationService.register(request), "Tạo tài khoản thành công!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request){
        ApiResponse<?> response =
                ApiResponse.success(authenticationService.refreshToken(request));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request){
        authenticationService.logout(request);
        ApiResponse<?> response = ApiResponse.success("Đăng xuất thành công!");
        return ResponseEntity.ok(response);
    }

}
