package com.khangmoihocit.VocabFlow.modules.auth.services;

import com.khangmoihocit.VocabFlow.modules.auth.dtos.request.AuthenticationRequest;
import com.khangmoihocit.VocabFlow.modules.auth.dtos.response.AuthenticationResponse;

public interface AuthenticationService {
    AuthenticationResponse authentication(AuthenticationRequest request);
}
