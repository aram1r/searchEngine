package searchengine.services.indexService.htmlSeparatorService;

import lombok.*;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Component;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.services.indexService.taskPools.Task;
import searchengine.services.indexService.taskPools.TaskPool;
import searchengine.services.wordService.WordServiceImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//TODO написать код для сохранения данных в таблицу индекс
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class SeparationLemmaTaskImpl extends Task {

    private HashMap<Page, SeparationLemmaTaskImpl> subtasks;

    private ConcurrentHashMap<Page, HashMap<String, Integer>> indexes;

    private static LuceneMorphology luceneMorphology;

    private static WordServiceImpl wordServiceImpl;

    private boolean parent;

    private ConcurrentHashMap<String, Lemma> result;

    private static LemmaRepository lemmaRepository;

    private static IndexRepository indexRepository;

    @Autowired
    public void setWordServiceImpl(WordServiceImpl wordServiceImpl) {
        SeparationLemmaTaskImpl.wordServiceImpl = wordServiceImpl;
    }

    @Autowired
    public void setIndexRepository(IndexRepository indexRepository) {
        SeparationLemmaTaskImpl.indexRepository = indexRepository;
    }

    @Autowired
    public void setLemmaRepository(LemmaRepository lemmaRepository) {
        SeparationLemmaTaskImpl.lemmaRepository = lemmaRepository;
    }

    public SeparationLemmaTaskImpl(Site site, TaskPool taskPool) {
        super(site, taskPool);
        parent = true;
        subtasks = new HashMap<>();
        result = new ConcurrentHashMap<>();
        indexes = new ConcurrentHashMap<>();
    }

    public SeparationLemmaTaskImpl(boolean parent, Page page, Site site, TaskPool taskPool,
                                   ConcurrentHashMap<String, Lemma> result, HashMap<Page,
            SeparationLemmaTaskImpl> subtasks, ConcurrentHashMap<Page, HashMap<String, Integer>> indexes) {
        super(site, taskPool, page);
        this.parent = parent;
        this.result = result;
        this.subtasks = subtasks;
        this.indexes = indexes;
    }

    @Override
    protected void compute() {
        if (parent) {
            initializeSeparation();
            collectResults(subtasks);
        } else {
            if (!getExecutorService().isShutdown()) {
                //Получаем body из страницы
                String text = Jsoup.clean(Jsoup.parseBodyFragment(getPage().getContent()).getElementsByTag("body").get(0).toString(),
                        Safelist.simpleText());

                addPageToIndexes();

                List<String> words = Arrays.asList(text.toLowerCase(Locale.ROOT).split("\\s+"));
                try {
                    List<String> wordsToProcess = new ArrayList<>();
                    words.forEach(e -> {
                        if (!wordServiceImpl.isLink(e)) {
                            wordsToProcess.add(e.replaceAll("[\\d=+/'@&%," +
                                    "\"<>!|·\\[\\]\\-_$(){}#©\\s?:;.]+", ""));
                        }
                    });
                    wordsToProcess.removeAll(Arrays.asList("", null));
                    wordsToProcess.forEach(e-> {
                        if (e.length()>2) {
                            //Переводим слово строку в нижний регистр
                            String word = e.toLowerCase();
                            word = wordServiceImpl.getNormalForm(word);
                            if (word!=null) {
                                putOrIncreaseFrequency(word);
                            }
                        }
                    });

                } catch (Exception ignored) {

                }
            }
        }
    }

    private void putOrIncreaseFrequency(String word) {
        //Добавляем в таблицу результат
        if (!result.containsKey(word)) {
            result.put(word, new Lemma(word, 1, getSite()));
        } else {
            increaseFrequency(word);
        }
        //Добавляем в таблицу indexes
        if (!indexes.get(getPage()).containsKey(word)) {
            indexes.get(getPage()).put(word, 1);
        } else {
            indexes.get(getPage()).remove(word);
            indexes.get(getPage()).put(word, indexes.get(getPage()).get(word)+1);
        }
    }

    private void addPageToIndexes() {
        if(indexes.isEmpty() || !indexes.contains(getPage())) {
            indexes.put(getPage(), new HashMap<>());
        }
    }

    private void initializeSeparation() {
        Iterable<Page> pages = getPageRepository().findAllBySite(getSite());
        for (Page page : pages) {
            SeparationLemmaTaskImpl separatorService = new SeparationLemmaTaskImpl(false, page, getSite(),
                    getTaskPool(), result, subtasks, indexes);
            subtasks.put(page, separatorService);
        }
        subtasks.forEach((k, v) -> v.fork());
    }

    private void increaseFrequency(String word) {
        Lemma lemma = result.get(word);
        lemma.setFrequency(lemma.getFrequency()+1);
    }

        private void collectResults(HashMap<Page, SeparationLemmaTaskImpl> subtasks) {
            subtasks.forEach((k, v) -> {
                if (!getTaskPool().isShutdown()) {
                    v.join();
                }
            });
            saveLemmas();
            saveIndexes();
            getSite().setStatus(Status.INDEXED);
            getSiteRepository().save(getSite());
    }

    private void saveLemmas() {
        for (Lemma lemma : result.values()) {
            try {
                if (lemma!=null) {
                    lemma.setId(null);
                    lemmaRepository.save(lemma);
                }
            } catch (Exception e) {
                System.out.println(e);
            }

        }
        System.out.println("Lemmas saved");
    }

    private void saveIndexes() {
        List<Index> indexList = Collections.synchronizedList(new ArrayList<>());
        try {
            HashMap<String, Lemma> lemmas = getLemmasMap();
            indexes.forEach((k, v) -> {
                if (v!=null && k!=null) {
                    v.forEach((kk ,vv) -> {
                        if (lemmas.get(kk)!=null) {
                            indexList.add(new Index(k, lemmas.get(kk), (float) vv));
                        }
                    });
                }
            });
            indexRepository.saveAll(indexList);
        } catch (Exception e) {
            System.out.println("Exception during index saving " +  e.getMessage() + " " + e.getClass());
        }
    }

    private HashMap<String, Lemma> getLemmasMap() {
        HashMap<String, Lemma> lemmas = new HashMap<>();
        List<Lemma> lemmaList = new ArrayList<>();
        try {
            lemmaList = lemmaRepository.findAllBySite(getSite());
        } catch (Exception e) {
            System.out.println(e.getMessage() + " Exception during loading lemma entities from db");
        }
        lemmaList.forEach(e -> lemmas.put(e.getLemma(), e));
        return lemmas;
    }
}
