import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexService.IndexService;

import java.time.LocalDateTime;

@SpringBootTest(classes = {Application.class, Page.class, Site.class})
public class IndexingServiceTest {

    private PageRepository pageRepository;


    private SiteRepository siteRepository;
    private IndexService indexService;
    private SitesList sitesList;


    public Site site;
    public Page page;

    @Autowired
    public void setPageRepository(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Autowired
    public void setSiteRepository(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Autowired
    public void setIndexService(IndexService indexService) {
        this.indexService = indexService;
    }

    @Autowired
    public void setSitesList(SitesList sitesList) {
        this.sitesList = sitesList;
    }

    @BeforeEach
    public void setup() {
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        site = new Site();
        site.setName("test");
        site.setStatus(Status.FAILED);
        site.setUrl("test.com");
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);

        page = new Page();
        page.setSite(site);
        page.setContent("asdf");
        page.setPath("asdf");
        page.setResponseCode(400);
        pageRepository.save(page);

        page = new Page();
        page.setSite(site);
        page.setContent("asdf");
        page.setPath("asdfa");
        page.setResponseCode(400);
        pageRepository.save(page);
    }

    @Test
    public void deleteSites(){
        indexService.deleteSite(page.getSite());
    }

    @Test
    public void saveSite() {
        Site site = sitesList.getSites().get(0);
        indexService.saveSite(new Site(Status.INDEXING, LocalDateTime.now(), null, site.getUrl(), site.getName()));
    }

}
