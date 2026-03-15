package com.khangmoihocit.VocabFlow.modules.vocabulary.controllers;

import com.khangmoihocit.VocabFlow.core.response.ApiResponse;
import com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.request.LookupRequest;
import com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.request.UserSaveWordRequest;
import com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.response.LookupResponse;
import com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.response.UserSavedWordResponse;
import com.khangmoihocit.VocabFlow.modules.vocabulary.services.DictionaryWordService;
import com.khangmoihocit.VocabFlow.modules.vocabulary.services.UserSavedWordService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
@RequestMapping("${spring.api.prefix}/vocabularies")
public class VocabularyController {
    UserSavedWordService userSavedWordService;
    DictionaryWordService dictionaryWordService;

    @PostMapping("/save-word-user")
    ResponseEntity<?> savedWordToUser(@Valid @RequestBody UserSaveWordRequest request){
        ApiResponse<UserSavedWordResponse> response =
                ApiResponse.success(userSavedWordService.savedWord(request), "Lưu từ vựng vào sổ tay của bạn thành công!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lookup/basic")
    public ResponseEntity<ApiResponse<LookupResponse>> lookupBasic(@RequestParam String word) {
        LookupResponse response = dictionaryWordService.lookupBasic(word);
        return ResponseEntity.ok(ApiResponse.success(response, "Tra cứu cơ bản thành công"));
    }

    // API 2: Dùng POST cho truy vấn AI (cần gửi chữ + câu ngữ cảnh dài)
    @PostMapping("/lookup/ai")
    public ResponseEntity<ApiResponse<LookupResponse>> lookupWithAi(@Valid @RequestBody LookupRequest request) {
        LookupResponse response = dictionaryWordService.lookupWithAi(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tra cứu bằng AI thành công"));
    }
}
