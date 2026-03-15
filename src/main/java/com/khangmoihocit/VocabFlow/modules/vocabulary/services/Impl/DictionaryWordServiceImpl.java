package com.khangmoihocit.VocabFlow.modules.vocabulary.services.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khangmoihocit.VocabFlow.core.exception.OurException;
import com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.GeminiWordInfo;
import com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.request.LookupRequest;
import com.khangmoihocit.VocabFlow.modules.vocabulary.dtos.response.LookupResponse;
import com.khangmoihocit.VocabFlow.modules.vocabulary.entities.DictionaryWord;
import com.khangmoihocit.VocabFlow.modules.vocabulary.mappers.DictionaryWordMapper;
import com.khangmoihocit.VocabFlow.modules.vocabulary.repositories.DictionaryWordRepository;
import com.khangmoihocit.VocabFlow.modules.vocabulary.services.DictionaryWordService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j(topic = "DICTIONARY WORD SERVICE")
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class DictionaryWordServiceImpl implements DictionaryWordService {

    DictionaryWordRepository dictionaryWordRepository;
    ChatClient chatClient;
    RestClient restClient;
    ObjectMapper objectMapper = new ObjectMapper();
    DictionaryWordMapper dictionaryWordMapper = new DictionaryWordMapper();

    public DictionaryWordServiceImpl(DictionaryWordRepository dictionaryWordRepository, ChatClient.Builder chatClientBuilder) {
        this.dictionaryWordRepository = dictionaryWordRepository;
        this.chatClient = chatClientBuilder.build();
        this.restClient = RestClient.create();
    }

    @Override
    public LookupResponse lookupBasic(String word) {
        String cleanWord = word.trim().toLowerCase();

        //Kiểm tra Database (Nếu có ai đó từng tra bằng AI rồi thì hưởng sái luôn)
        Optional<DictionaryWord> existingWordOpt = dictionaryWordRepository.findFirstByWord(cleanWord);
        if (existingWordOpt.isPresent()) {
            return dictionaryWordMapper.mapToResponse(existingWordOpt.get());
        }

        WordData dictData = fetchFromDictionaryApi(cleanWord);
        String finalAudioUrl = (dictData.audioUrl() != null) ? dictData.audioUrl() : generateGoogleTtsUrl(cleanWord);

        DictionaryWord newWord = DictionaryWord.builder()
                .word(cleanWord)
                .partOfSpeech(dictData.partOfSpeech() != null ? dictData.partOfSpeech() : "unknown")
                .pronunciation(dictData.phonetic())
                .meaningVi("Đang cập nhật!") // Lời nhắc cho Frontend
                .explanationEn(dictData.explanationEn())
                .audioUrl(finalAudioUrl)
                .build();

        newWord = dictionaryWordRepository.save(newWord);
        return dictionaryWordMapper.mapToResponse(newWord);
    }

    @Override
    public LookupResponse lookupWithAi(LookupRequest request) {
        String cleanWord = request.getWord().trim().toLowerCase();
        WordData aiData = fetchFromGeminiApi(cleanWord, request.getContextSentence());
        WordData dictData = fetchFromDictionaryApi(cleanWord);

        String finalAudioUrl = (dictData.audioUrl() != null) ? dictData.audioUrl() : generateGoogleTtsUrl(cleanWord);
        String finalPhonetic = (dictData.phonetic() != null) ? dictData.phonetic() : aiData.phonetic();

        // Tìm trong DB xem cặp (Word + Từ loại mới này) đã có chưa.
        // Dùng orElseGet để nếu có thì update (nếu cần), nếu chưa thì tạo mới.
        DictionaryWord savedWord = dictionaryWordRepository.findByWordAndPartOfSpeech(cleanWord, aiData.partOfSpeech())
                .orElseGet(() -> {
                    DictionaryWord newWord = DictionaryWord.builder()
                            .word(cleanWord)
                            .partOfSpeech(aiData.partOfSpeech())
                            .pronunciation(finalPhonetic)
                            .meaningVi(aiData.meaningVi())
                            .explanationEn(aiData.explanationEn() != null ? aiData.explanationEn() : dictData.explanationEn())
                            .audioUrl(finalAudioUrl)
                            .build();
                    return dictionaryWordRepository.save(newWord);
                });

        if (savedWord.getMeaningVi().contains("Đang cập nhật!")) {
            savedWord.setMeaningVi(aiData.meaningVi());
            savedWord.setExplanationEn(aiData.explanationEn());
            savedWord.setPronunciation(finalPhonetic);
            dictionaryWordRepository.save(savedWord);
        }

        return dictionaryWordMapper.mapToResponse(savedWord);
    }

    private WordData fetchFromDictionaryApi(String word) {
        String phonetic = null;
        String audioUrl = null;
        String partOfSpeech = null;
        String meaningVi = "Không tìm thấy nghĩa trong từ điển gốc"; // Sẽ bị Gemini ghi đè sau
        String explanationEn = null;

        try {
            var response = restClient.get()
                    .uri("https://api.dictionaryapi.dev/api/v2/entries/en/{word}", word)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode body = objectMapper.readTree(response.getBody());
                if (body.isArray() && !body.isEmpty()) {
                    JsonNode firstEntry = body.get(0);

                    // Lấy âm thanh và phiên âm
                    if (firstEntry.has("phonetic") && !firstEntry.get("phonetic").isNull()) {
                        phonetic = firstEntry.get("phonetic").asText();
                    }
                    JsonNode phoneticsArray = firstEntry.get("phonetics");
                    if (phoneticsArray != null && phoneticsArray.isArray()) {
                        for (JsonNode node : phoneticsArray) {
                            if (phonetic == null && node.has("text")) phonetic = node.get("text").asText();
                            if (node.has("audio") && !node.get("audio").asText().isEmpty()) {
                                audioUrl = node.get("audio").asText();
                                break;
                            }
                        }
                    }

                    JsonNode meaningsArray = firstEntry.get("meanings");
                    if (meaningsArray != null && meaningsArray.isArray() && !meaningsArray.isEmpty()) {
                        JsonNode firstMeaning = meaningsArray.get(0);
                        if (firstMeaning.has("partOfSpeech")) partOfSpeech = firstMeaning.get("partOfSpeech").asText();
                        JsonNode definitions = firstMeaning.get("definitions");
                        if (definitions != null && definitions.isArray() && !definitions.isEmpty()) {
                            explanationEn = definitions.get(0).get("definition").asText();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Dictionary API bỏ qua từ: {}", word);
        }
        return new WordData(partOfSpeech, phonetic, meaningVi, explanationEn, audioUrl);
    }

    private WordData fetchFromGeminiApi(String word, String contextSentence) {
        GeminiWordInfo aiInfo = chatClient.prompt()
                .user(u -> u.text("Bạn là chuyên gia ngôn ngữ. Phân tích '{word}' trong câu: '{context}'. Trả về JSON với các key: partOfSpeech, phonetic, meaningVi, explanationEn.")
                        .param("word", word)
                        .param("context", contextSentence != null ? contextSentence : ""))
                .call()
                .entity(GeminiWordInfo.class);

        return new WordData(aiInfo.partOfSpeech(), aiInfo.phonetic(), aiInfo.meaningVi(), aiInfo.explanationEn(), null);
    }

    private String generateGoogleTtsUrl(String text) {
        try {
            // Mã hóa URL để chống lỗi dấu cách (VD: "get ahead" -> "get+ahead")
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            return "https://translate.google.com/translate_tts?ie=UTF-8&q=" + encodedText + "&tl=en&client=tw-ob";
        } catch (Exception e) {
            return null;
        }
    }

    private record WordData(String partOfSpeech, String phonetic, String meaningVi, String explanationEn, String audioUrl) {}
}