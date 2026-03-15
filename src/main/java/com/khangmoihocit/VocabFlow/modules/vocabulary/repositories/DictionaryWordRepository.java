package com.khangmoihocit.VocabFlow.modules.vocabulary.repositories;

import com.khangmoihocit.VocabFlow.modules.vocabulary.entities.DictionaryWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DictionaryWordRepository extends JpaRepository<DictionaryWord, Long> {

    Optional<DictionaryWord> findByWordAndPartOfSpeech(String word, String partOfSpeech);
    Optional<DictionaryWord> findFirstByWord(String word);
}
