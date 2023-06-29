package searchengine.services.wordProcessorService;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WordProcessorService {

    private final EnglishLuceneMorphology englishLuceneMorphology;
    private final RussianLuceneMorphology russianLuceneMorphology;

    public boolean isWord(String word) {
        return word.matches("[A-Za-zА-Яа-я\\d]+");
    }

    public boolean isRussianWord(String word) {
        return !russianLuceneMorphology.getMorphInfo(word).get(0).contains("СОЮЗ") &&
                !russianLuceneMorphology.getMorphInfo(word).get(0).contains("ПРЕДЛ") &&
                !russianLuceneMorphology.getMorphInfo(word).get(0).contains("МЕЖД") &&
                !russianLuceneMorphology.getMorphInfo(word).get(0).contains("ЧАСТ") &&
                !russianLuceneMorphology.getMorphInfo(word).get(0).contains("МС");
    }

    public boolean isEnglishWord(String word) {
        return !englishLuceneMorphology.getMorphInfo(word).get(0).contains("CONJ") &&
                !englishLuceneMorphology.getMorphInfo(word).get(0).contains("PN") &&
                !englishLuceneMorphology.getMorphInfo(word).get(0).contains("ADVERB");
    }

    public boolean isLink(String word) {
        return word.matches("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]" +
                "{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");
    }
}
