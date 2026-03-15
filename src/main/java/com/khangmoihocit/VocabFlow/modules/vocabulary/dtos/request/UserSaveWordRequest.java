package com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSaveWordRequest {
    @NotNull(message = "từ/cụm từ không được để trống")
    Long dictionaryWordId;
    String sourceSentence;
    String sourceUrl;
}
