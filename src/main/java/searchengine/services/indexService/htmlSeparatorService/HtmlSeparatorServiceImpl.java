package searchengine.services.indexService.htmlSeparatorService;

import lombok.*;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.services.indexService.taskPools.URLTaskPool;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class HtmlSeparatorServiceImpl extends RecursiveAction {

    private Site site;
    private Page page;

    private static HashMap <Page, HtmlSeparatorServiceImpl> subtasks;

    private static Set<Page> tasksInWork;

    private static PageRepository pageRepository;
    private LuceneMorphology luceneMorphology;

    boolean parent;

    private HashMap<String, Lemma> result;

    private static LemmaRepository lemmaRepository;

    private static AtomicBoolean isActive;

    private static URLTaskPool urlTaskPool;

    @Autowired
    public void setUrlTaskPool(URLTaskPool urlTaskPool) {
        HtmlSeparatorServiceImpl.urlTaskPool = urlTaskPool;
    }

    @Autowired
    public void setPageRepository(PageRepository pageRepository) {
        HtmlSeparatorServiceImpl.pageRepository = pageRepository;
    }
    @Autowired
    public void setLemmaRepository(LemmaRepository lemmaRepository) {
        HtmlSeparatorServiceImpl.lemmaRepository = lemmaRepository;
    }

    public HtmlSeparatorServiceImpl(Site site) {
        this.site = site;
        parent = true;
        tasksInWork = Collections.synchronizedSet(new HashSet<>());
        subtasks = new HashMap<>();
        result = new HashMap<>();
        isActive = new AtomicBoolean(true);
    }

    public HtmlSeparatorServiceImpl(boolean parent, Page page, Site site) {
        this.parent = parent;
        this.page = page;
        this.site = site;
        this.result = new HashMap<>();
    }


    private boolean ifWord(String word) {
        return !luceneMorphology.getMorphInfo(word).get(0).contains("СОЮЗ") &&
                !luceneMorphology.getMorphInfo(word).get(0).contains("ПРЕДЛ") &&
                !luceneMorphology.getMorphInfo(word).get(0).contains("МЕЖД") &&
                !luceneMorphology.getMorphInfo(word).get(0).contains("ЧАСТ") &&
                !luceneMorphology.getMorphInfo(word).get(0).contains("МС");
    }

    @Override
    public void compute() {
        if (parent) {
            initializeSeparation();
        } else {
            tasksInWork.add(page);
            String text = page.getContent();

//            Site site = page.getSite();
//            HashMap<String, Lemma> result = new HashMap<>();
            List<String> words = Arrays.asList(text.toLowerCase(Locale.ROOT).split("\\s+"));
            try {
                luceneMorphology = new RussianLuceneMorphology();
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
                                result.put(word, new Lemma(word, 1, site));
                            } else {
                                increaseFrequency(word);
                            }
                        }
                    }
                });

            } catch (Exception ignored) {
//                System.out.println(ignored.getMessage());
            }
        }
        lemmaRepository.saveAll(result.values());
        if (isActive.get()) {
            finishSeparation();
        }
//        if (subtasks.size()==tasksInWork.size() || urlTaskPool.getActiveThreadCount()==0 || urlTaskPool.isQuiescent()) {
////            collectResults(subtasks);
////            HashSet<Lemma> lemmas = new HashSet<>();
////            result.forEach((k, v) -> {
////                lemmas.add(v);
////            });
////            lemmaRepository.saveAll(lemmas);
//            site.setStatus(Status.INDEXED);
//        }
    }

    private boolean isLink(String word) {
        return word.matches("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
    }

    private void initializeSeparation() {
        Iterable<Page> pages = pageRepository.findAllBySite(site);
        for (Page page : pages) {
            HtmlSeparatorServiceImpl separatorService = new HtmlSeparatorServiceImpl(false, page, site);
            separatorService.fork();
            subtasks.put(page, separatorService);
        }
        subtasks.forEach((k, v) -> {
            urlTaskPool.execute(v);
        });
    }

    private synchronized void finishSeparation() {
        if (subtasks.size()==tasksInWork.size() || urlTaskPool.getActiveThreadCount()==0 || urlTaskPool.isQuiescent()) {
            isActive.set(false);
            site.setStatus(Status.INDEXED);
        }
    }
    private void increaseFrequency(String word) {
        Lemma lemma = result.get(word);
        lemma.setFrequency(lemma.getFrequency()+1);
        result.put(word, lemma);
    }

    private void collectResults(HashMap<Page, HtmlSeparatorServiceImpl> subtasks) {
        try {
            subtasks.forEach((k, v) -> v.join());
        } catch (Exception e) {
//            logger.warn("Ошибка при Join " + e.getMessage());
        }
    }
}
