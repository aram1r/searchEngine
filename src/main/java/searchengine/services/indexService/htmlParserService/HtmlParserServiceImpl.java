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
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HtmlParserServiceImpl extends RecursiveAction implements HtmlParserService{

    private Site site;
    private Page page;
    private HashMap<String, Page> result;
    private boolean parent;
    private static AppProps appProps;
    private PageRepository pageRepository;
    private Logger logger;
    private static AtomicInteger threadCount;

    @Autowired
    public void setLogger() {
        this.logger = LoggerFactory.getLogger("HtmlParser");
    }

    @Autowired
    public void setPageRepository(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Autowired
    public void setAppProps(AppProps appProps) {
        HtmlParserServiceImpl.appProps = appProps;
    }

    public HtmlParserServiceImpl(Site site) {
        this.site = site;
        this.page = new Page();
        page.setPath("/");
        result = new HashMap<>();
        parent = true;
        threadCount = new AtomicInteger();
    }

    public HtmlParserServiceImpl(Site site, HashMap<String, Page> result) {
        this.site = site;
        this.page = new Page();
        page.setPath("/");
        this.result = result;
        threadCount.getAndIncrement();
    }

    public HtmlParserServiceImpl(Site site, Page page, HashMap<String, Page> result) {
        this.site = site;
        this.page = page;
        this.result = result;
        threadCount.getAndIncrement();
    }

    @Override
    protected void compute() {
        //Считаем количество слэшей в текущем адресе
        if (!result.containsKey(page.getPath())) {
            processPage(page.getPath());
            page.setPath(page.getPath().replace(site.getUrl(), "/"));
            HashMap<String, HtmlParserServiceImpl> subTasks = new HashMap<>();
            long countBackslash = page.getPath().chars().filter(ch -> ch == '/').count();
            result.put(page.getPath(), page);
            forkURLs(page, countBackslash, subTasks);
            collectResults(subTasks);
        }
        if (parent) {
            pageRepository.saveAll(result.values());
        }
        threadCount.getAndDecrement();
    }

    //TODO Попробовать сделать subtasks общим для одного сайта, посмотреть на время выполнения
//    @Override
//    protected void compute() {
//        //Считаем количество слэшей в текущем адресе
//        if (!result.containsKey(page.getPath())) {
//            processPage(page.getPath());
//            page.setPath(page.getPath().replace(site.getUrl(), "/"));
//            HashMap<String, HtmlParserServiceImpl> subTasks = new HashMap<>();
//            long countBackslash = page.getPath().chars().filter(ch -> ch == '/').count();
//            result.put(page.getPath(), page);
//            forkURLs(page, countBackslash, subTasks);
//            collectResults(subTasks);
//        }
//        if (parent) {
//            pageRepository.saveAll(result.values());
//        }
//    }

    private void forkURLs(Page page, long countBackslash, HashMap<String, HtmlParserServiceImpl> subtasks) {
        if (page.getResponseCode()==200) {
            extractLinks(subtasks, countBackslash, page);
            subtasks.forEach((k, v) -> v.fork());
        }
    }

    private void extractLinks(HashMap<String, HtmlParserServiceImpl> subTasks, long countBackslash, Page page) {
        Document document = Jsoup.parse(page.getContent(), site.getUrl());
        Elements links = document.select("a[href]");
        for (Element element : links) {
            String urlLink = element.absUrl("href");
            if (urlLink.contains(site.getUrl()) && validUrl(subTasks, countBackslash, urlLink)) {
                urlLink = urlLink.replace(site.getUrl(), "");
                subTasks.put(urlLink, new HtmlParserServiceImpl(site, new Page(urlLink), result));
            }
        }
    }

    private boolean validUrl(HashMap<String, HtmlParserServiceImpl> subTasks, long countBackslash, String urlLink) {
        if (urlLink.chars().filter(ch -> ch == '/').count()>= countBackslash) {
            Page page = new Page(urlLink.replace(site.getUrl(), ""));
            page.setSite(site);
            if (!result.containsKey(page.getPath()) && !subTasks.containsKey(urlLink)) {
                return urlLink.endsWith("/") || urlLink.endsWith("html");
            }
        }
        return false;
    }

    private void processPage (String url) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(501, 5000));
            Connection.Response response = Jsoup.connect(site.getUrl()+url).userAgent(appProps.getUserAgent())
                    .referrer(appProps.getReferrer()).execute();
            page.setSite(site);
            page.setResponseCode(response.statusCode());
            if (page.getResponseCode()==200) {
                page.setContent(response.parse().toString());
            } else {
                page.setContent("");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void collectResults(HashMap<String, HtmlParserServiceImpl> subtasks) {
        try {
            subtasks.forEach((k, v) -> v.join());
        } catch (Exception e) {
            logger.warn("Ошибка при Join " + e.getMessage());
        }
    }
}
