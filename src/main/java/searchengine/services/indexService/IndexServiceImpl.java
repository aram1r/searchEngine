package searchengine.services.indexService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexService.htmlParserService.HtmlParserService;
import searchengine.services.indexService.htmlParserService.HtmlParserServiceImpl;
import searchengine.services.indexService.htmlSeparatorService.HtmlSeparatorServiceImpl;

import java.time.LocalDateTime;

@Service
public class IndexServiceImpl implements IndexService{

    URLTaskPool urlTaskPool;
    SitesList sitesList;
    HtmlSeparatorServiceImpl htmlSeparatorService;
    SiteRepository siteRepository;

    @Autowired
    public void setSiteRepository(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Autowired
    public void setUrlTaskPool(URLTaskPool urlTaskPool) {
        this.urlTaskPool = urlTaskPool;
    }


    @Autowired
    public void setSitesList(SitesList sitesList) {
        this.sitesList = sitesList;
    }

    @Autowired
    public void setHtmlSeparatorService(HtmlSeparatorServiceImpl htmlSeparatorService) {
        this.htmlSeparatorService = htmlSeparatorService;
    }

    @Override
    public ResponseEntity<String> startIndexing() {
        for (Site site : sitesList.getSites()) {
            deleteSite(site);
            saveSite(site);
            indexSite(site);
        }
        return null;
    }

    public synchronized void saveSite(Site site) {
        siteRepository.save(new Site(Status.INDEXING, LocalDateTime.now(), null, site.getUrl(), site.getName()));
    }


    public synchronized void deleteSite(Site site) {
        siteRepository.deleteByUrl(site.getUrl());
    }

    @Override
    public void indexSite(Site site) {
        HtmlParserServiceImpl htmlParserService = new HtmlParserServiceImpl();
        htmlParserService.setSite(site);
        urlTaskPool.submit(htmlParserService);
    }
}
