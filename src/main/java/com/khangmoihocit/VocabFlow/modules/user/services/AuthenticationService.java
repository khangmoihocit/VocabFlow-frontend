package com.khangmoihocit.VocabFlow.modules.user.services;

import com.khangmoihocit.VocabFlow.modules.user.dtos.request.AuthenticationRequest;
import com.khangmoihocit.VocabFlow.modules.user.dtos.request.RefreshTokenRequest;
import com.khangmoihocit.VocabFlow.modules.user.dtos.request.UserCreationRequest;
import com.khangmoihocit.VocabFlow.modules.user.dtos.response.AuthenticationResponse;
import com.khangmoihocit.VocabFlow.modules.user.dtos.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;

public interface AuthenticationService {
    AuthenticationResponse authentication(AuthenticationRequest request);
    UserResponse register(UserCreationRequest request);
    AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
}
