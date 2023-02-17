import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexService.htmlSeparatorService.HtmlSeparatorServiceImpl2;
import searchengine.services.indexService.taskPools.URLTaskPool;

@SpringBootTest(classes = {Application.class})
public class HtmlSeparatorServiceImplTest {

    private SiteRepository siteRepository;

    private LemmaRepository lemmaRepository;

    private static URLTaskPool urlTaskPool;

    private Site site;

    @Autowired
    public void setUrlTaskPool(URLTaskPool urlTaskPool) {
        HtmlSeparatorServiceImplTest.urlTaskPool = urlTaskPool;
    }

    @Autowired
    public void setSiteRepository(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }
    @Autowired
    public void setLemmaRepository(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    @BeforeEach
    public void setup() {
        site = siteRepository.findAll().iterator().next();
        lemmaRepository.deleteAllBySite(site);
    }

    @Test
    void separateLemmasTest() {
        HtmlSeparatorServiceImpl2 htmlSeparatorService = new HtmlSeparatorServiceImpl2(site);
        site.setStatus(Status.INDEXING);
        siteRepository.save(site);
        urlTaskPool.submit(htmlSeparatorService);
    }
}
