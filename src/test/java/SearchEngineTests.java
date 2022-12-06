
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;


@SpringBootTest(classes = {Application.class, Page.class, Site.class})
public class SearchEngineTests {


    private PageRepository pageRepository;


    private SiteRepository siteRepository;


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
    void deleteSite() {
        Iterable<Site> sites = siteRepository.findAll();
        AtomicInteger length = new AtomicInteger();
        for (Site site1 : sites) {
            Iterable<Page> pages = pageRepository.findAll();
            pages.forEach(e -> {
                length.getAndIncrement();
            });
            System.out.println(length);
            siteRepository.delete(site1);
            pages = pageRepository.findAll();
            length.set(0);
            pages.forEach(e -> {
                length.getAndIncrement();
            });
            System.out.println(length);
        }

        assert length.get()==0;
    }

    @Test
    void deletePage() {
        pageRepository.deleteAll();
        Iterable<Site> iterator = siteRepository.findAll();
        AtomicInteger length = new AtomicInteger();
        iterator.forEach(e-> {
            length.getAndIncrement();
        });
        assert length.get()!=0;
    }

    @Test
    void testOneToOnePageSite() {
        Site site2 = page.getSite();
        assert  site2!=null;
    }
}
