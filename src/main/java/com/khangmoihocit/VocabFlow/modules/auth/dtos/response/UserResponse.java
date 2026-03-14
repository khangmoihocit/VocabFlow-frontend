package com.khangmoihocit.VocabFlow.modules.auth.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String email;
    String fullName;
    String role;
    String ankiDeckName;
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
