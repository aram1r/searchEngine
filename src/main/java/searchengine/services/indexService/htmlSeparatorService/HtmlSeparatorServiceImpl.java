package searchengine.services.indexService.htmlSeparatorService;

import lombok.*;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.services.indexService.htmlParserService.HtmlParserServiceImpl;
import searchengine.services.indexService.taskPools.URLTaskPool;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

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

    private ConcurrentHashMap<String, Lemma> result;

    private static LemmaRepository lemmaRepository;

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
        result = new ConcurrentHashMap<>();
    }

    public HtmlSeparatorServiceImpl(boolean parent, Page page, ConcurrentHashMap<String, Lemma> result) {
        this.parent = parent;
        this.page = page;
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
    public void compute() {
        if (parent) {
            initializeSeparation();
        } else {
            tasksInWork.add(page);
            String text = page.getContent();
            Site site = page.getSite();
            HashMap<String, Lemma> result = new HashMap<>();
            List<String> words = Arrays.asList(text.toLowerCase(Locale.ROOT).split("\\s+"));
            try {
                luceneMorphology = new RussianLuceneMorphology();
                words.forEach(e-> {
                    if (!isLatin(e)) {
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
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        if (subtasks.size()==tasksInWork.size()) {
            collectResults(subtasks);
            HashSet<Lemma> lemmas = new HashSet<>();
            result.forEach((k, v) -> {
                lemmas.add(v);
            });
            lemmaRepository.saveAll(lemmas);
            site.setStatus(Status.INDEXED);
        }
    }

    private boolean isLatin(String word) {
        boolean isLatin = word.matches("[(\\<(/?[^\\>]+)\\>) ]*[a-zA-Z0-9]+");
        return isLatin;
    }

    private void initializeSeparation() {
        Iterable<Page> pages = pageRepository.findAllBySite(site);
        for (Page page : pages) {
            HtmlSeparatorServiceImpl separatorService = new HtmlSeparatorServiceImpl(false, page, result);
            separatorService.fork();
            subtasks.put(page, separatorService);
        }
        subtasks.forEach((k, v) -> {
            urlTaskPool.execute(v);
        });
    }

    private synchronized void increaseFrequency(String word) {
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
