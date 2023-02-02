package searchengine.services.indexService.htmlSeparatorService;

import lombok.*;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

@Service
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HtmlSeparatorServiceImpl extends RecursiveAction {

    private Site site;
    private Page page;

    private HashMap <Page, HtmlSeparatorServiceImpl> subtasks;

    private Set<Page> tasksInWork;

    private PageRepository pageRepository;
    private LuceneMorphology luceneMorphology;

    boolean parent;

    private ConcurrentHashMap<String, Lemma> result;

    private LemmaRepository lemmaRepository;

    @Autowired
    public void setLemmaRepository(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    public HtmlSeparatorServiceImpl(Site site) {
        this.site = site;
        parent = true;
        tasksInWork = Collections.synchronizedSet(new HashSet<>());
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
            tasksInWork.add(page);
            String text = page.getContent();
            Site site = page.getSite();
            HashMap<String, Lemma> result = new HashMap<>();
            List<String> words = Arrays.asList(text.toLowerCase(Locale.ROOT).split("\\s+"));
            words.forEach(e-> {
                e = e.replaceAll("[?!:;,.]?", "");
                try {
                    String word = luceneMorphology.getNormalForms(e).get(0);
                    if (word.length()>1 && ifWord(word)) {
                        if (!result.containsKey(word)) {
                            Lemma lemma = result.get(word);
                            lemma.setFrequency(1);
                            result.put(word, new Lemma(word, 1, site));
                        } else {
                            increaseFrequency(word);
                        }
                    }
                } catch (Exception ignore) {

                }
            });
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

    private void initializeSeparation() {
        Iterable<Page> pages = pageRepository.findAll();
        for (Page page : pages) {
            HtmlSeparatorServiceImpl separatorService = new HtmlSeparatorServiceImpl();
            separatorService.setParent(false);
            separatorService.setPage(page);
            separatorService.fork();
            subtasks.put(page, separatorService);
        }
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
