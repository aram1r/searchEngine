package searchengine.services.indexService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.configuration.SitesList;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexService.htmlParserService.ParserTaskImpl;
import searchengine.services.indexService.htmlSeparatorService.SeparationLemmaTaskImpl;
import searchengine.services.indexService.taskPools.ExecuteThread;
import searchengine.services.indexService.taskPools.TaskPool;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService{

    final TaskPool taskPool;
    final SitesList sitesList;
    final SiteRepository siteRepository;
    final PageRepository pageRepository;

    final LemmaRepository lemmaRepository;

    final ExecutorService executorService;

    final IndexRepository indexRepository;

//    @Autowired
//    public void setExecutorService(ExecutorService executorService) {
//        this.executorService = executorService;
//    }
//
//    @Autowired
//    public void setLemmaRepository(LemmaRepository lemmaRepository) {
//        this.lemmaRepository = lemmaRepository;
//    }
//
//    @Autowired
//    public void setSiteRepository(SiteRepository siteRepository) {
//        this.siteRepository = siteRepository;
//    }
//
//    @Autowired
//    public void setUrlTaskPool(TaskPool taskPool) {
//        this.taskPool = taskPool;
//    }
//
//    @Autowired
//    public void setSitesList(SitesList sitesList) {
//        this.sitesList = sitesList;
//    }
//
//
//    @Autowired
//    public void setPageRepository(PageRepository pageRepository) {
//        this.pageRepository = pageRepository;
//    }

    //TODO придумать как начинать обработку лемм на страницах, как понять что закончен парсинг того или иного сайта
    //во времени.
    @Override
    public ResponseEntity<String> startIndexing() {
        deleteAllIndexes();
        deleteAllLemmas();
        deleteAllPages();
        deleteAllSites();
        for (Site site : sitesList.getSites()) {
            saveSite(site);
        }
        for (Site site : siteRepository.findAll()) {
            indexSite(site);
        }
        return null;
    }

    public ResponseEntity<String> stopIndexing() {
        executorService.shutdown();
        taskPool.shutdown();
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
        ParserTaskImpl htmlParserService = new ParserTaskImpl(site, new TaskPool());
        executorService.submit(new ExecuteThread(htmlParserService));
//        taskPool.submit(htmlParserService);
    }

    @Override
    public ResponseEntity<String> startSeparation() {
        List<Site> siteList = siteRepository.findAll();
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        for (Site site : siteList) {
            SeparationLemmaTaskImpl htmlSeparatorService = new SeparationLemmaTaskImpl(site, new TaskPool());
            site.setStatus(Status.INDEXING);
            siteRepository.save(site);
            executorService.submit(new ExecuteThread(htmlSeparatorService));
        }
        return null;
    }


    public void deleteAllIndexes() {indexRepository.deleteAll();}
    public void deleteAllSites() {
        siteRepository.deleteAll();
    }

    public void deleteAllPages() {pageRepository.deleteAll();}

    public void deleteAllLemmas() {
        lemmaRepository.deleteAll();
    }
}
