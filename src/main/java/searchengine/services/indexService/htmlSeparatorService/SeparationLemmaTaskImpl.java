package searchengine.services.indexService.htmlSeparatorService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.services.indexService.taskPools.Task;
import searchengine.services.indexService.taskPools.TaskPool;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//TODO написать код для сохранения данных в таблицу индекс
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class SeparationLemmaTaskImpl extends Task {

    private static HashMap<Page, SeparationLemmaTaskImpl> subtasks;

    private static LuceneMorphology luceneMorphology;

    private boolean parent;

    private ConcurrentHashMap<String, Lemma> result;

    private static LemmaRepository lemmaRepository;


    @Autowired
    public void setLemmaRepository(LemmaRepository lemmaRepository) {
        SeparationLemmaTaskImpl.lemmaRepository = lemmaRepository;
    }
    @Autowired
    public void setLuceneMorphology(LuceneMorphology luceneMorphology) {
        SeparationLemmaTaskImpl.luceneMorphology = luceneMorphology;
    }

    public SeparationLemmaTaskImpl(Site site, TaskPool taskPool) {
        super(site, taskPool);
        parent = true;
        subtasks = new HashMap<>();
        result = new ConcurrentHashMap<>();
    }

    public SeparationLemmaTaskImpl(boolean parent, Page page, Site site, TaskPool taskPool,
                                   ConcurrentHashMap<String, Lemma> result) {
        super(site, taskPool, page);
        this.parent = parent;
        this.result = result;
    }

    private boolean ifWord(String word) {
        return !luceneMorphology.getMorphInfo(word).get(0).contains("СОЮЗ") &&
                !luceneMorphology.getMorphInfo(word).get(0).contains("ПРЕДЛ") &&
                !luceneMorphology.getMorphInfo(word).get(0).contains("МЕЖД") &&
                !luceneMorphology.getMorphInfo(word).get(0).contains("ЧАСТ") &&
                !luceneMorphology.getMorphInfo(word).get(0).contains("МС");
    }

    @Override
    protected void compute() {
        if (parent) {
            initializeSeparation();
            collectResults(subtasks);
        } else {
            if (!getExecutorService().isShutdown()) {
                String text = getPage().getContent();
                List<String> words = Arrays.asList(text.toLowerCase(Locale.ROOT).split("\\s+"));
                try {
                    List<String> wordsToProcess = new ArrayList<>();
                    words.forEach(e -> {
                        if (!isLink(e)) {
                            wordsToProcess.add(e.replaceAll("[A-Za-z0-9=+/;:.'@&%,\"<>!|·\\[\\]\\-_$(){}#©\s]+", ""));
                        }
                    });
                    wordsToProcess.removeAll(Arrays.asList("", null));
                    wordsToProcess.forEach(e-> {
                        if (e.length()>2) {
                            String word = luceneMorphology.getNormalForms(e.replaceAll("[?!:;,.]?", "")).get(0);
                            if (word.length()>1 && ifWord(word)) {
                                if (!result.containsKey(word)) {
                                    result.put(word, new Lemma(word, 1, getSite()));
                                } else {
                                    increaseFrequency(word);
                                }
                            }
                        }
                    });

                } catch (Exception ignored) {

                }
            }
        }
    }

    private boolean isLink(String word) {
        return word.matches("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");
    }

    private void initializeSeparation() {
        Iterable<Page> pages = getPageRepository().findAllBySite(getSite());
        for (Page page : pages) {
            SeparationLemmaTaskImpl separatorService = new SeparationLemmaTaskImpl(false, page, getSite(),
                    getTaskPool(), result);
            subtasks.put(page, separatorService);
        }
        subtasks.forEach((k, v) -> v.fork());
    }

    private void increaseFrequency(String word) {
        Lemma lemma = result.get(word);
        lemma.setFrequency(lemma.getFrequency()+1);
//        result.put(word, lemma);
    }

        private void collectResults(HashMap<Page, SeparationLemmaTaskImpl> subtasks) {
        try {
            subtasks.forEach((k, v) -> {
                if (!getTaskPool().isShutdown()) {
                    v.join();
                }
            });
            lemmaRepository.saveAll(result.values());
            getSite().setStatus(Status.INDEXED);
            getSiteRepository().save(getSite());
        } catch (Exception e) {
            getLogger().warn(e.getMessage());
            System.out.println(e.getMessage());
        }
    }
}
