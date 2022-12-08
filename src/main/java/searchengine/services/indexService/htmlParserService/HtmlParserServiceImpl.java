package searchengine.services.indexService.htmlParserService;

import lombok.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.config.AppProps;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HtmlParserServiceImpl extends RecursiveAction implements HtmlParserService{

    private Site site;
    private Page page;
    private HashSet<Page> result;

    private static AppProps appProps;

    @Value("${html-parser-service-impl.user-agent}")
    private String userAgent;

    private static PageRepository pageRepository;

    @Autowired
    public void setAppProps(AppProps appProps) {
        HtmlParserServiceImpl.appProps = appProps;
    }

    @Autowired
    public void setPageRepository(PageRepository pageRepository) {
        HtmlParserServiceImpl.pageRepository = pageRepository;
    }

    public HtmlParserServiceImpl(Site site) {
        this.site = site;
        this.page = new Page();
        page.setPath(site.getUrl());
        result = new HashSet<>();
    }

    @Override
    protected void compute() {
        processPage(page.getPath());
        savePage(page);
//        HashSet<Page> subtasks = new HashSet<>();
//        //Считаем количество слэшей в текущем адресе
//        long countBackslash = site.getUrl().chars().filter(ch -> ch == '/').count();
//        if (!result.contains(page)) {
//            result.add(page);
//            forkURLs(page, countBackslash);
//        }

    }

    private void forkURLs(Page page, long countBackslash) {

    }

    private void processPage (String url) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(501, 5000));
            Connection.Response response = Jsoup.connect(url).userAgent(appProps.getUserAgent())
                    .referrer(appProps.getReferrer()).execute();
            page.setPath(url.replace(site.getUrl(), ""));
            page.setSite(site);
            page.setResponseCode(response.statusCode());
            if (page.getResponseCode()==200) {
                page.setContent(response.parse().toString());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Synchronized
    private void savePage(Page page) {
        pageRepository.save(page);
        for (Page page1 : pageRepository.findAll()) {
            System.out.println(page1.toString());
        }
    }
}
