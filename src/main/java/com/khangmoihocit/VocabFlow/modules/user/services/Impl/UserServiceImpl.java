package com.khangmoihocit.VocabFlow.modules.user.services.Impl;

import com.khangmoihocit.VocabFlow.core.enums.ErrorCode;
import com.khangmoihocit.VocabFlow.core.exception.AppException;
import com.khangmoihocit.VocabFlow.core.response.PageResponse;
import com.khangmoihocit.VocabFlow.core.specification.BaseSpecification;
import com.khangmoihocit.VocabFlow.core.specification.GenericSpecification;
import com.khangmoihocit.VocabFlow.core.specification.GenericSpecificationBuilder;
import com.khangmoihocit.VocabFlow.core.specification.SearchCriteria;
import com.khangmoihocit.VocabFlow.core.utils.SortUtil;
import com.khangmoihocit.VocabFlow.modules.user.dtos.response.UserResponse;
import com.khangmoihocit.VocabFlow.modules.user.entities.User;
import com.khangmoihocit.VocabFlow.modules.user.mappers.UserMapper;
import com.khangmoihocit.VocabFlow.modules.user.repositories.UserRepository;
import com.khangmoihocit.VocabFlow.modules.user.services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    UserMapper userMapper;

    @Override
    public List<UserResponse> getAll() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) throw new AppException(ErrorCode.USER_IS_EMPTY);
        return users.stream().map(userMapper::toUserResponse).toList();
    }

    @Override
    public PageResponse<UserResponse> getUsers(Map<String, String[]> parameters) {
        int page = parameters.containsKey("page") ? Integer.parseInt(parameters.get("page")[0]) : 1;
        int size = parameters.containsKey("size") ? Integer.parseInt(parameters.get("size")[0]) : 20;
        String sortParam = parameters.containsKey("sort") ? parameters.get("sort")[0] : null;
        Sort sort = SortUtil.createSort(sortParam);
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        String keyword = parameters.containsKey("keyword") ? parameters.get("keyword")[0] : null;
        String role = parameters.containsKey("role") ? parameters.get("role")[0] : null;
        Boolean isActive = parameters.containsKey("isActive") ? Boolean.valueOf(parameters.get("isActive")[0]) : null;

        GenericSpecificationBuilder<User> builder = new GenericSpecificationBuilder<>();
        if (role != null && !role.isEmpty()) builder.with("role", "=", role);
        if (isActive != null) builder.with("isActive", "=", isActive);

        Specification<User> finalSpec = Specification.where(builder.build());
        if (keyword != null && !keyword.trim().isEmpty()) {
            Specification<User> searchSpec = BaseSpecification
                    .keywordSpec(keyword, "email", "fullName");

            // WHERE (role = ? AND is_active = ?) AND (email LIKE ? OR full_name LIKE ?)
            finalSpec = finalSpec.and(searchSpec);
        }

        Page<User> userPage = userRepository.findAll(finalSpec, pageable);
        List<UserResponse> userResponses = new ArrayList<>();
        if(!userPage.getContent().isEmpty()){
            userResponses = userMapper.toListUserResponse(userPage.getContent());
        }

        return PageResponse.<UserResponse>builder()
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .data(userResponses)
                .build();
    }
}
