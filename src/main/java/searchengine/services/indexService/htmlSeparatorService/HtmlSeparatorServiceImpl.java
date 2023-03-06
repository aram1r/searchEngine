package searchengine.services.indexService.htmlSeparatorService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
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
import searchengine.services.indexService.htmlParserService.HtmlParserServiceImpl;
import searchengine.services.indexService.taskPools.Task;
import searchengine.services.indexService.taskPools.TaskPool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RecursiveAction;

//TODO переписать реализацию на fork join, без этого не отловить финальную точку
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class HtmlSeparatorServiceImpl extends Task {

    private Site site;
    private Page page;

    private TaskPool taskPool;

    private static ExecutorService executorService;
    private static HashMap<Page, HtmlSeparatorServiceImpl> subtasks;

    private static Set<Page> tasksInWork;

    private static PageRepository pageRepository;

    private static LuceneMorphology luceneMorphology;

    private boolean parent;

    private static final ConcurrentHashMap<String, Lemma> result = new ConcurrentHashMap<>();

    private static LemmaRepository lemmaRepository;

    private static SiteRepository siteRepository;

    private static Boolean isActive;

    private static Logger logger;

    @Autowired
    public void setExecutorService(ExecutorService executorService) {
        HtmlSeparatorServiceImpl.executorService = executorService;
    }

    @Autowired
    public void setLogger() {
        logger = LoggerFactory.getLogger("HtmlSeparator");
    }

    @Autowired
    public void setPageRepository(PageRepository pageRepository) {
        HtmlSeparatorServiceImpl.pageRepository = pageRepository;
    }
    @Autowired
    public void setLemmaRepository(LemmaRepository lemmaRepository) {
        HtmlSeparatorServiceImpl.lemmaRepository = lemmaRepository;
    }
    @Autowired
    public void setLuceneMorphology(LuceneMorphology luceneMorphology) {
        HtmlSeparatorServiceImpl.luceneMorphology = luceneMorphology;
    }
    @Autowired
    public void setSiteRepository(SiteRepository siteRepository) {
        HtmlSeparatorServiceImpl.siteRepository = siteRepository;
    }

    public HtmlSeparatorServiceImpl(Site site, TaskPool taskPool) {
        this.taskPool = taskPool;
        this.site = site;
        parent = true;
        tasksInWork = Collections.synchronizedSet(new HashSet<>());
        subtasks = new HashMap<>();
        isActive = true;
    }

    public HtmlSeparatorServiceImpl(boolean parent, Page page, Site site, TaskPool taskPool) {
        this.taskPool = taskPool;
        this.parent = parent;
        this.page = page;
        this.site = site;
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
            if (!executorService.isShutdown()) {
                String text = page.getContent();
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
                                    result.put(word, new Lemma(word, 1, site));
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
        Iterable<Page> pages = pageRepository.findAllBySite(site);
        for (Page page : pages) {
            HtmlSeparatorServiceImpl separatorService = new HtmlSeparatorServiceImpl(false, page, site, taskPool);
            subtasks.put(page, separatorService);
        }
        subtasks.forEach((k, v) -> {
            v.fork();
        });
    }

    private void increaseFrequency(String word) {
        Lemma lemma = result.get(word);
        lemma.setFrequency(lemma.getFrequency()+1);
        result.put(word, lemma);
    }

        private void collectResults(HashMap<Page, HtmlSeparatorServiceImpl> subtasks) {
        try {
            subtasks.forEach((k, v) -> {
                if (!taskPool.isShutdown()) {
                    v.join();
                }
            });
            lemmaRepository.saveAll(result.values());
            site.setStatus(Status.INDEXED);
            siteRepository.save(site);
        } catch (Exception ignored) {
            logger.warn(ignored.getMessage());
            System.out.println(ignored.getMessage());
        }
    }
}
