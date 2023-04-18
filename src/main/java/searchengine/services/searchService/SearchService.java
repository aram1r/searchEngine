package searchengine.services.searchService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SitePageLemmaRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    final SitePageLemmaRepository lemmaPageRepository;

    final LemmaRepository lemmaRepository;
    final IndexRepository indexRepository;
    public ResponseEntity<SearchResult> search(String query, Integer offset, Integer limit, String site) {
        SearchQuery searchQuery = new SearchQuery(query, offset, limit);
        if (site != null) {
            searchQuery.setSites(new ArrayList<>());
            searchQuery.getSites().add(site);
        }
        getPages(searchQuery);
        return null;
    }

    public List<Page> getPages(SearchQuery searchQuery) {
        List<SitePageLemma> sitePageLemmaList = new ArrayList<>();
        lemmaPageRepository.findAllBySiteID(3);
        List<Page> result = new ArrayList<>();
        List<Integer> pageIndexes = new ArrayList<>();
        List<String> words = new ArrayList<>(Arrays.asList(searchQuery.getQuery().split("\\s+")));
        for (String word : words) {
            //TODO оптимизировать
            Lemma lemma = lemmaRepository.findLemmaByLemma(word);
            List<Index> indexes = indexRepository.findAllByLemmaId(lemma.getId());
            if (pageIndexes.isEmpty()) {
                indexes.forEach(e -> pageIndexes.add(e.getPage().getId()));
            } else {
                indexes.stream().filter(a -> (!pageIndexes.contains(a.getPage().getId()))).
                        forEach(e -> pageIndexes.remove(e.getPage().getId()));
            }
        }
        return result;
    }
}
