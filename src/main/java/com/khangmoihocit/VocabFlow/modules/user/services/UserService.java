package com.khangmoihocit.VocabFlow.modules.user.services;

import com.khangmoihocit.VocabFlow.core.response.PageResponse;
import com.khangmoihocit.VocabFlow.modules.user.dtos.response.UserResponse;
import com.khangmoihocit.VocabFlow.modules.user.entities.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface UserService {
    List<UserResponse> getAll();
    PageResponse<UserResponse> getUsers(Map<String, String[]> parameters);
}
