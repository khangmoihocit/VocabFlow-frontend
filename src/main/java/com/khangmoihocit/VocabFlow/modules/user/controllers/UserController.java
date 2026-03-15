package com.khangmoihocit.VocabFlow.modules.user.controllers;

import com.khangmoihocit.VocabFlow.core.response.ApiResponse;
import com.khangmoihocit.VocabFlow.core.response.PageResponse;
import com.khangmoihocit.VocabFlow.modules.user.dtos.response.UserResponse;
import com.khangmoihocit.VocabFlow.modules.user.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
@RequestMapping("${spring.api.prefix}/user")
public class UserController {
    UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<?> getAll(HttpServletRequest request){
        Map<String, String[]> parameters = request.getParameterMap();
        PageResponse<UserResponse> pageResponse = userService.getUsers(parameters);
        ApiResponse<PageResponse<UserResponse>> response = ApiResponse.success(pageResponse);

        return ResponseEntity.ok(response);
    }
}
