import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.services.wordService.WordServiceImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = {Application.class, Lemma.class, Site.class})
public class WordServiceTest {

    public List<Lemma> lemmaList = new ArrayList<>();

    public WordServiceImpl wordServiceImpl;

    @Autowired
    public void setWordServiceImpl(WordServiceImpl wordServiceImpl) {
        this.wordServiceImpl = wordServiceImpl;
    }

    @BeforeEach
    public void setup() {
        Site site = new Site();
        site.setName("none");
        lemmaList.add(new Lemma("собаку", 1, site));
        lemmaList.add(new Lemma("apples", 1, site));
        lemmaList.add(new Lemma("кошку", 1, site));
        lemmaList.add(new Lemma("abracadabra123", 1, site));
        lemmaList.add(new Lemma("сон", 1, site));
        lemmaList.add(new Lemma("фывафвыаячсм45", 1, site));
        lemmaList.add(new Lemma("cat1", 1, site));



    }

    @Test
    public void checkWords() {
        int count=0;
        for (Lemma e : lemmaList) {
            if (wordServiceImpl.isWord(e.getLemma())) {
                count++;
            }
        }
        assert (count==4);
    }

    @Test
    public void checkEnglishWords() {
        int count = 0;
        for (Lemma e : lemmaList) {
            if (wordServiceImpl.isEnglishWord(e.getLemma())) {
                count++;
            }
        }
        assert (count==1);
    }

    @Test
    public void getNormalRussianForms() {
        Assertions.assertEquals(wordServiceImpl.getNormalForm(lemmaList.get(0).getLemma()), "собака");
        Assertions.assertEquals(wordServiceImpl.getNormalForm(lemmaList.get(1).getLemma()), "apple");
    }

    @Test
    public void getNormalFormsError() {
        String word = wordServiceImpl.getNormalForm("фывафывам");
        System.out.println(word);
    }
}
