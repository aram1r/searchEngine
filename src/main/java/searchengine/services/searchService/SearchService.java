package searchengine.services.searchService;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    final SiteRepository siteRepository;
    final LemmaRepository lemmaRepository;
    final IndexRepository indexRepository;

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
        List<Integer> pageIndexes = new ArrayList<>();
        List<String> words = new ArrayList<>(Arrays.asList(searchQuery.getQuery().split("\\s+")));
        for (Site site : searchQuery.getSites()) {
            for (String word : words) {
                //TODO оптимизировать
                List<Index> indexes = new ArrayList<>();
                Lemma lemma = lemmaRepository.findLemmaBySiteIdAndLemma(site.getId(), luceneMorphology.getNormalForms(word).get(0));

                if (lemma!=null) {
                    List<Page> pageList = pageRepository.findAllByContentIsContaining(lemma.getLemma());

//                    List<Index> tempIndex = indexRepository.findAllByLemmaAndPage(lemma, page);

//                    indexes.addAll(indexRepository.findAllByLemmaIdAndSiteId
//                            (site.getId(), lemma.getId()));
                    if (pageIndexes.isEmpty()) {
                        indexes.forEach(e -> pageIndexes.add(e.getPage().getId()));
                    } else {
                        indexes.stream().filter(a -> (!pageIndexes.contains(a.getPage().getId()))).
                                forEach(e -> pageIndexes.remove(e.getPage().getId()));
                    }
                }

            }
        }

        return result;
    }
}
