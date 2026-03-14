package com.khangmoihocit.VocabFlow.modules.user.services;

import com.khangmoihocit.VocabFlow.modules.user.dtos.response.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> getAll();
}
