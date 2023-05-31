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
        getPages(searchQuery);
        return null;
    }

    public List<Page> getPages(SearchQuery searchQuery) {
        List<Page> result = new ArrayList<>();
        List<String> words = new ArrayList<>(Arrays.asList(searchQuery.getQuery().toLowerCase().split("\\s+")));
        removeNotWords(words);

        for (Site site : searchQuery.getSites()) {
            Integer pagesOnSite = pageRepository.countAllBySite(site);
            for (String word : words) {
                //TODO оптимизировать
                Lemma lemma = lemmaRepository.findLemmaBySiteIdAndLemma(site.getId(), luceneMorphology.getNormalForms(word).get(0));

                if (lemma!=null) {
                    //TODO переписать запрос чтобы находил страницы содержащие необходимое слово (возможно необходимо заходить через индексрепозитори)
                    List<Page> pageList = pageRepository.findAllBySiteAndContentLike(site, lemma.getLemma());
                    if (pageList.size()!=0 && pageList.size()<pagesOnSite/10) {
                        //                    List<Index> tempIndex = indexRepository.findAllByLemmaAndPage(lemma, page);

//                    indexes.addAll(indexRepository.findAllByLemmaIdAndSiteId
//                            (site.getId(), lemma.getId()));
                        if (result.isEmpty()) {
                            result.addAll(pageList);
                        } else {
                            result.forEach(e -> {
                                if (!pageList.contains(e)) {
                                    result.remove(e);
                                }
                            });
                        }
                    }
                }

            }
        }
        return result;
    }

    private void removeNotWords(List<String> words) {
        words.removeIf(word -> !wordProcessorService.ifWord(word));
    }
}
