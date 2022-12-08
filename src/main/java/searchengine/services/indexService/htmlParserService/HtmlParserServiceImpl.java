package searchengine.services.indexService.htmlParserService;

import lombok.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.AppProps;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import java.util.HashMap;
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


    private static PageRepository pageRepository;

    private Logger logger;

    @Autowired
    public void setLogger() {
        this.logger = LoggerFactory.getLogger("HtmlParser");;
    }

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

    public HtmlParserServiceImpl(Site site, Page page, HashSet<Page> result) {
        this.site = site;
        this.page = page;
        this.result = result;
    }

    @Override
    protected void compute() {
        processPage(page.getPath());
        savePage(page);
        HashMap<String, HtmlParserServiceImpl> subTasks = new HashMap<>();
        //Считаем количество слэшей в текущем адресе
        long countBackslash = page.getPath().chars().filter(ch -> ch == '/').count();
        if (!result.contains(page)) {
            result.add(page);
            forkURLs(page, countBackslash, subTasks);
            collectResults(subTasks);
        }

    }

    private void forkURLs(Page page, long countBackslash, HashMap<String, HtmlParserServiceImpl> subtasks) {
        if (page.getResponseCode()==200) {
            extractLinks(subtasks, countBackslash, page);
            subtasks.forEach((k, v) -> {
                v.fork();
            });
        }
    }

    private void extractLinks(HashMap<String, HtmlParserServiceImpl> subTasks, long countBackslash, Page page) {
        Document document = new Document(page.getContent());
        Elements links = document.select("href");
//        Elements links = new Document(page.getContent()).select("href");
        for (Element element : links) {
            String urlLink = element.attr("abs:href");
            if (validUrl(subTasks, countBackslash, urlLink)) {
                subTasks.put(urlLink, new HtmlParserServiceImpl(site, new Page(urlLink), result));
            }
//            if (urlLink.contains(site.getUrl())) {
//                if (urlLink.chars().filter(ch -> ch == '/').count()>= countBackslash) {
//                    if (!result.contains(urlLink) && !subTasks.containsKey(urlLink)) {
//                        if ((urlLink.endsWith("/") || urlLink.endsWith("html"))) {
//                            subTasks.put(urlLink, new HtmlParserServiceImpl(site, new Page(urlLink), result));
//                        }
//                    }
//                }
//            }
        }
    }

    private boolean validUrl(HashMap<String, HtmlParserServiceImpl> subTasks, long countBackslash, String urlLink) {
        if (urlLink.contains(site.getUrl()) && urlLink.chars().filter(ch -> ch == '/').count()>= countBackslash) {
            Page page = new Page(urlLink);
            page.setSite(site);
            if (!result.contains(page) && !subTasks.containsKey(urlLink)) {
                return urlLink.endsWith("/") || urlLink.endsWith("html");
            }
        }
        return false;
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

    private void collectResults(HashMap<String, HtmlParserServiceImpl> subtasks) {
        try {
            subtasks.forEach((k, v) -> {
                v.join();
            });
        } catch (Exception e) {
            logger.warn("Ошибка при Join " + e.getMessage());
        }
    }

    @Synchronized
    private void savePage(Page page) {
        pageRepository.save(page);
    }
}
