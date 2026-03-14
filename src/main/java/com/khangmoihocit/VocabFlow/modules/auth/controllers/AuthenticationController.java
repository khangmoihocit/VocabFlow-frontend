package com.khangmoihocit.VocabFlow.modules.auth.controllers;

import com.khangmoihocit.VocabFlow.core.response.ApiResponse;
import com.khangmoihocit.VocabFlow.modules.auth.dtos.request.AuthenticationRequest;
import com.khangmoihocit.VocabFlow.modules.auth.dtos.response.AuthenticationResponse;
import com.khangmoihocit.VocabFlow.modules.auth.services.AuthenticationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        ApiResponse<AuthenticationResponse> response = ApiResponse.success(authenticationResponse, "Đăng nhập thành công!");
        return ResponseEntity.ok(response);
    }


}
