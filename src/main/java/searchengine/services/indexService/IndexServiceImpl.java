package searchengine.services.indexService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexService.htmlParserService.HtmlParserServiceImpl;
import searchengine.services.indexService.htmlSeparatorService.HtmlSeparatorServiceImpl;
import searchengine.services.indexService.taskPools.URLTaskPool;

import java.time.LocalDateTime;

@Service
public class IndexServiceImpl implements IndexService{

    URLTaskPool urlTaskPool;
    SitesList sitesList;
    HtmlSeparatorServiceImpl htmlSeparatorService;
    SiteRepository siteRepository;
    PageRepository pageRepository;


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

    @Autowired
    public void setPageRepository(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    //TODO придумать как начинать обработку лемм на страницах, как понять что закончен парсинг того или иного сайта
    //во времени.
    @Override
    public ResponseEntity<String> startIndexing() {
        deleteAllSites();
        for (Site site : sitesList.getSites()) {
            saveSite(site);
        }
        for (Site site : siteRepository.findAll()) {
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

    public synchronized Site getSiteByURL(Site site) {
        return siteRepository.getSiteByUrl(site.getUrl());
    }

    public void indexSite(Site site) {
        site.setStatus(Status.INDEXING);
        siteRepository.save(site);
        HtmlParserServiceImpl htmlParserService = new HtmlParserServiceImpl(site);
        urlTaskPool.submit(htmlParserService);
    }

    public void deleteAllSites() {
        siteRepository.deleteAll();
    }
}
