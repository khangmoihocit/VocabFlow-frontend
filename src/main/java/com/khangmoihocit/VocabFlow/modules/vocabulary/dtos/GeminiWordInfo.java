package com.khangmoihocit.VocabFlow.modules.vocabulary.dtos;

// Record là một tính năng của Java 14+ giúp viết class DTO siêu ngắn
public record GeminiWordInfo(
        String partOfSpeech,
        String phonetic,
        String meaningVi,
        String explanationEn
) {}