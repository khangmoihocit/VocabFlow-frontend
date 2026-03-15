package com.khangmoihocit.VocabFlow.modules.vocabulary.mappers;

import com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.response.LookupResponse;
import com.khangmoihocit.VocabFlow.modules.vocabulary.entities.DictionaryWord;

public class DictionaryWordMapper {
    public DictionaryWordMapper(){}
    public LookupResponse mapToResponse(DictionaryWord word) {
        return LookupResponse.builder()
                .dictionaryWordId(word.getId())
                .word(word.getWord())
                .partOfSpeech(word.getPartOfSpeech())
                .phonetic(word.getPronunciation())
                .meaningVi(word.getMeaningVi())
                .explanationEn(word.getExplanationEn())
                .audioUrl(word.getAudioUrl())
                .build();
    }
}
