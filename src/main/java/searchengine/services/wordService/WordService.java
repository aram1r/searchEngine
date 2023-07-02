package searchengine.services.wordService;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WordService {

    private final EnglishLuceneMorphology englishLuceneMorphology;
    private final RussianLuceneMorphology russianLuceneMorphology;

    public boolean isWord(String word) {
        return word.matches("[A-Za-zА-Яа-я]+");
    }

    public boolean isRussianWord(String word) {
        if (word.matches("[А-Яа-я]+")) {
            return !russianLuceneMorphology.getMorphInfo(word).get(0).contains("СОЮЗ") &&
                    !russianLuceneMorphology.getMorphInfo(word).get(0).contains("ПРЕДЛ") &&
                    !russianLuceneMorphology.getMorphInfo(word).get(0).contains("МЕЖД") &&
                    !russianLuceneMorphology.getMorphInfo(word).get(0).contains("ЧАСТ") &&
                    !russianLuceneMorphology.getMorphInfo(word).get(0).contains("МС");
        } else {
            return false;
        }
    }

    public boolean isEnglishWord(String word) {
        if (word.matches("[A-Za-z]+")) {
            return !englishLuceneMorphology.getMorphInfo(word).get(0).contains("CONJ") &&
                    !englishLuceneMorphology.getMorphInfo(word).get(0).contains("PN") &&
                    !englishLuceneMorphology.getMorphInfo(word).get(0).contains("ADVERB");
        } else {
            return false;
        }
    }

    public boolean isLink(String word) {
        return word.matches("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]" +
                "{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");
    }

    private List<String> getNormalEnglishForms (String word) {
        return englishLuceneMorphology.getNormalForms(word);
    }

    private List<String> getNormalRussianForms (String word) {
        return russianLuceneMorphology.getNormalForms(word);
    }

    public String getNormalForm (String word) {
        if (isWord(word)) {
            if (isEnglishWord(word)) {
                return getNormalEnglishForms(word).get(0);
            } else if (isRussianWord(word)) {
                return getNormalRussianForms(word).get(0);
            }
        }
        return null;
    }
}
