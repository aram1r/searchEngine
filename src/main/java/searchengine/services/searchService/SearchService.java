package searchengine.services.searchService;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.wordProcessorService.WordProcessorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    final SiteRepository siteRepository;
    final LemmaRepository lemmaRepository;
    final IndexRepository indexRepository;
    final WordProcessorService wordProcessorService;

    final PageRepository pageRepository;
    final LuceneMorphology luceneMorphology;
    public ResponseEntity<SearchResult> search(String query, Integer offset, Integer limit, String site) {
        SearchQuery searchQuery = new SearchQuery(query, offset, limit, new ArrayList<>());
        if (site != null) {
            Site siteFromDB = siteRepository.findAllByUrl(site);
            searchQuery.getSites().add(siteFromDB);
        } else {
            siteRepository.findAll().forEach(e->{
                searchQuery.getSites().add(e);
            });
        }
        List<Page> resultPages = getPages(searchQuery);

        //TODO создать метод с списком объектов которые должны уйти в итоге на фронт
        getSnippets(resultPages);
        return null;
    }

    private void getSnippets(List<Page> resultPages) {
    }

    public List<Page> getPages(SearchQuery searchQuery) {
        List<Page> result = new ArrayList<>();
        List<Index> indexResult = new ArrayList<>();
        List<String> words = new ArrayList<>(Arrays.asList(searchQuery.getQuery().toLowerCase().split("\\s+")));
        removeNotWords(words);
        for (Site site : searchQuery.getSites()) {
            //Получаем леммы из бд
            List<Lemma> lemmaList = getLemmasFromDB(words, site);

            if (lemmaList.size()!=0) {
                //Сортировка лемм по частоте
                sortLemmasByFrequency(lemmaList);
                //Получаем количество страниц на сайте, для сравнения на каком количестве страниц встречаются леммы
                // относительно всех страниц
                Integer pagesOnSite = pageRepository.countAllBySite(site);
                for (Lemma lemma : lemmaList) {
                    if (lemma!=null) {
                        List<Index> indexList = indexRepository.findAllByLemma_SiteAndLemma(site, lemma);
                        if (indexList.size()!=0 && indexList.size()<pagesOnSite/10) {
                            if (indexResult.isEmpty()) {
                                indexResult.addAll(indexList);
                            } else {
                                indexResult.forEach(e -> {
                                    if (!indexList.contains(e)) {
                                        indexResult.remove(e);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
        for (Index index : indexResult) {
            result.add(index.getPage());
        }
        //TODO отсортировать старницы по релевантности
        return result;
    }

    private List<Lemma> getLemmasFromDB(List<String> words, Site site) {
        ArrayList<Lemma> lemmata = new ArrayList<>();
        for (String word : words) {
            Lemma lemma = lemmaRepository.findLemmaBySiteIdAndLemma(site.getId(), luceneMorphology.getNormalForms(word).get(0));
            if (lemma!=null) {
                lemmata.add(lemma);
            }
        }
        return lemmata;
    }

    private void sortLemmasByFrequency(List<Lemma> lemmas) {
        Collections.sort(lemmas);
    }

    private void removeNotWords(List<String> words) {
        words.removeIf(word -> !wordProcessorService.ifWord(word));
    }
}
