package searchengine.services.wordService;

import java.util.List;

public interface WordService {
    boolean isWord(String word);

    boolean isRussianWord(String word);

    boolean isEnglishWord(String word);

    boolean isLink(String word);

    //TODO почему-то не доходят русские слова
    String getNormalForm(String word);
}
