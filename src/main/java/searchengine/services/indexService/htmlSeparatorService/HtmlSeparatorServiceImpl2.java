package searchengine.services.indexService.htmlSeparatorService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexService.taskPools.URLTaskPool;

import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

//TODO переписать реализацию на fork join, без этого не отловить финальную точку
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class HtmlSeparatorServiceImpl2 extends RecursiveAction {

    private Site site;
    private Page page;

    private static HashMap<Page, HtmlSeparatorServiceImpl2> subtasks;

    private static Set<Page> tasksInWork;

    private static PageRepository pageRepository;

    private LuceneMorphology luceneMorphology;

    private boolean parent;

    private HashMap<String, Lemma> result;

    private static LemmaRepository lemmaRepository;

    private static SiteRepository siteRepository;

    private static AtomicBoolean isActive;

    private static URLTaskPool urlTaskPool;

    private static Logger logger;

    @Autowired
    public void setLogger() {
        logger = LoggerFactory.getLogger("HtmlSeparator");
    }

    @Autowired
    public void setUrlTaskPool(URLTaskPool urlTaskPool) {
        HtmlSeparatorServiceImpl2.urlTaskPool = urlTaskPool;
    }

    @Autowired
    public void setPageRepository(PageRepository pageRepository) {
        HtmlSeparatorServiceImpl2.pageRepository = pageRepository;
    }
    @Autowired
    public void setLemmaRepository(LemmaRepository lemmaRepository) {
        HtmlSeparatorServiceImpl2.lemmaRepository = lemmaRepository;
    }

    public HtmlSeparatorServiceImpl2(Site site) {
        this.site = site;
        parent = true;
        tasksInWork = Collections.synchronizedSet(new HashSet<>());
        subtasks = new HashMap<>();
        result = new HashMap<>();
        isActive = new AtomicBoolean(true);
    }

    public HtmlSeparatorServiceImpl2(boolean parent, Page page, Site site) {
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
    protected void compute() {
        if (parent) {
            initializeSeparation();
        } else {
            String text = page.getContent();
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

            }
            synchronized (this) {
                lemmaRepository.saveAll(result.values());
            }
        }
        collectResults(subtasks);
//        finishSeparation();
    }

    private boolean isLink(String word) {
        return word.matches("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");
    }

    private void initializeSeparation() {
        Iterable<Page> pages = pageRepository.findAllBySite(site);
        for (Page page : pages) {
            HtmlSeparatorServiceImpl2 separatorService = new HtmlSeparatorServiceImpl2(false, page, site);
            subtasks.put(page, separatorService);
        }
        subtasks.forEach((k, v) -> {
            v.fork();
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

        private void collectResults(HashMap<Page, HtmlSeparatorServiceImpl2> subtasks) {
        try {
            subtasks.forEach((k, v) -> v.join());
            isActive.set(false);
            site.setStatus(Status.INDEXED);
            siteRepository.save(site);
        } catch (Exception e) {
            logger.warn("Ошибка при Join " + e.getMessage());
        }
    }
}
