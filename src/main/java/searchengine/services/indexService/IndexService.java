package searchengine.services.indexService;

import org.springframework.http.ResponseEntity;
import searchengine.model.Site;


public interface IndexService {
    ResponseEntity<String> startIndexing();

    void deleteSite(Site site);

    void saveSite(Site site);

    void indexSite(Site site);
}
