package searchengine.services.indexService.htmlParserService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.configuration.AppProps;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.services.indexService.htmlSeparatorService.HtmlSeparatorServiceImpl;
import searchengine.services.indexService.taskPools.URLTaskPool;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Service
public class HtmlParserServiceImpl extends RecursiveAction implements HtmlParserService{


    private static URLTaskPool urlTaskPool;
    private Site site;
    private Page page;
    private ConcurrentHashMap<String, Page> result;
    private static AppProps appProps;

    private static PageRepository pageRepository;
    private static Logger logger;
    private Set<String> tasksInWork;
    private static Long timestamp;
    @Autowired
    public void setUrlTaskPool(URLTaskPool urlTaskPool) {
        HtmlParserServiceImpl.urlTaskPool = urlTaskPool;
    }
    @Autowired
    public void setAppProps(AppProps appProps) {
        HtmlParserServiceImpl.appProps = appProps;
    }

    @Autowired
    public void setLogger() {
        logger = LoggerFactory.getLogger("HtmlParser");
    }

    @Autowired
    public void setPageRepository(PageRepository pageRepository) {
        HtmlParserServiceImpl.pageRepository = pageRepository;
    }

    public HtmlParserServiceImpl(Site site) {
        this.site = site;
        this.page = new Page();
        page.setPath("/");
        result = new ConcurrentHashMap<>();
        tasksInWork = Collections.synchronizedSet(new HashSet<>());
        tasksInWork.add("/");
        timestamp = System.currentTimeMillis();
    }


    public HtmlParserServiceImpl(Site site, Page page, Set<String> tasksInWork, ConcurrentHashMap<String, Page> result) {
        this.site = site;
        this.page = page;
        this.tasksInWork = tasksInWork;
        this.result = result;
    }

    @Override
    protected void compute() {
//        Считаем количество слэшей в текущем адресе
        if (!result.containsKey(page.getPath())) {
            processPage(page.getPath());
            page.setPath(page.getPath().replace(site.getUrl(), "/"));
            HashMap<String, HtmlParserServiceImpl> subTasks = new HashMap<>();
            long countBackslash = page.getPath().chars().filter(ch -> ch == '/').count();
            result.put(page.getPath(), page);
            forkURLs(page, countBackslash, subTasks);
            collectResults(subTasks);
        }
        //Сохраняем результат
        if (result.size() == tasksInWork.size()) {
            System.out.println("Pasrsing took " + (System.currentTimeMillis()-timestamp)/60000 + " minutes");
            timestamp = System.currentTimeMillis();
            System.out.println(site.getName() + " parsing ended");
            pageRepository.saveAll(result.values());
            System.out.println("Saving took " + (System.currentTimeMillis()-timestamp)/60000 + " minutes");
            //Начинаем обработку лемм сайта
            separateLemmas();
        }
    }

    //TODO проверить не будет ли проблем из-за форка, может быть стоит сабмитить в таскпул
    private void separateLemmas() {
        HtmlSeparatorServiceImpl htmlSeparatorService = new HtmlSeparatorServiceImpl();
        htmlSeparatorService.setSite(site);
        urlTaskPool.submit(htmlSeparatorService);
    }

    private void forkURLs(Page page, long countBackslash, HashMap<String, HtmlParserServiceImpl> subtasks) {
        if (page.getResponseCode()==200) {
            extractLinks(subtasks, countBackslash, page);
            subtasks.forEach((k, v) -> {
                if (!tasksInWork.contains(k)) {
//                    v.fork();
                    urlTaskPool.execute(v);
                    tasksInWork.add(k);
                }
            });
        }
    }

    private void extractLinks(HashMap<String, HtmlParserServiceImpl> subTasks, long countBackslash, Page page) {
        Document document = Jsoup.parse(page.getContent(), site.getUrl());
        Elements links = document.select("a[href]");
        for (Element element : links) {
            String urlLink = element.absUrl("href");
            if (urlLink.contains(site.getUrl()) && validUrl(subTasks, countBackslash, urlLink)) {
                urlLink = urlLink.replace(site.getUrl(), "");
                subTasks.put(urlLink, new HtmlParserServiceImpl(site, new Page(urlLink), tasksInWork, result));
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