import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexService.htmlSeparatorService.HtmlSeparatorServiceImpl;
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
        HtmlSeparatorServiceImpl htmlSeparatorService = new HtmlSeparatorServiceImpl(site);
        urlTaskPool.submit(htmlSeparatorService);
    }
}
