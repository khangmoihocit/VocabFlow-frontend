package com.khangmoihocit.VocabFlow.modules.vocabulary.services;

import com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.request.LookupRequest;
import com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.response.LookupResponse;

public interface DictionaryWordService {
    LookupResponse lookupBasic(String word);
    LookupResponse lookupWithAi(LookupRequest request);
}
