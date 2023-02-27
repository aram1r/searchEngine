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
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexService.htmlSeparatorService.HtmlSeparatorServiceImpl;
import searchengine.services.indexService.taskPools.TaskPool;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;



//TODO проверить как работает join, скорей всего он собирает все результаты и не требуется проверка совпадения количества задач
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Service
public class HtmlParserServiceImpl extends RecursiveAction implements HtmlParserService{


    private static TaskPool taskPool;
    private Site site;
    private Page page;
    private ConcurrentHashMap<String, Page> result;
    private static AppProps appProps;

    private static PageRepository pageRepository;

    private static SiteRepository siteRepository;
    private static Logger logger;
    private static final HashSet<String> tasksInWork = new HashSet<>();
    private static Long timestamp;

    private boolean parent;

    @Autowired
    public void setTaskPool(TaskPool taskPool) {
        HtmlParserServiceImpl.taskPool = taskPool;
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

    @Autowired
    public void setSiteRepository(SiteRepository siteRepository) {
        HtmlParserServiceImpl.siteRepository = siteRepository;
    }

    public HtmlParserServiceImpl(Site site) {
        this.site = site;
        this.page = new Page();
        page.setPath("/");
        result = new ConcurrentHashMap<>();
        tasksInWork.add("/");
        timestamp = System.currentTimeMillis();
        parent = true;
    }


    public HtmlParserServiceImpl(Site site, Page page, ConcurrentHashMap<String, Page> result) {
        this.site = site;
        this.page = page;
        this.result = result;
        parent = false;
    }

    @Override
    protected void compute() {
        if (!taskPool.isShutdown()) {
            if (!result.containsKey(page.getPath())) {
                processPage(page.getPath());
                page.setPath(page.getPath().replace(site.getUrl(), "/"));
                long countBackslash = page.getPath().chars().filter(ch -> ch == '/').count();
                result.put(page.getPath(), page);
                HashMap<String, HtmlParserServiceImpl> subTasks = new HashMap<>();
                forkURLs(page, countBackslash, subTasks);
                collectResults(subTasks);
                //TODO Переписать замер времени на АОП и логгирование
                if (parent) {
                    System.out.println("Parsing took " + (System.currentTimeMillis()-timestamp)/60000 + " minutes");
                    timestamp = System.currentTimeMillis();
                    System.out.println(site.getName() + " parsing ended");
                    pageRepository.saveAll(result.values());
                    System.out.println("Saving took " + (System.currentTimeMillis()-timestamp)/60000 + " minutes");
                    if (!taskPool.isShutdown()) {
                        //Начинаем обработку лемм сайта
                        separateLemmas();
                    } else {
                        site.setStatus(Status.FAILED);
                        siteRepository.save(site);
                    }
                }
            }
//            if (result.size() == tasksInWork.size()) {
//                System.out.println("Pasrsing took " + (System.currentTimeMillis()-timestamp)/60000 + " minutes");
//                timestamp = System.currentTimeMillis();
//                System.out.println(site.getName() + " parsing ended");
//                pageRepository.saveAll(result.values());
//                System.out.println("Saving took " + (System.currentTimeMillis()-timestamp)/60000 + " minutes");
//                //Начинаем обработку лемм сайта
//                separateLemmas();
//            }
        }
    }

    //TODO проверить не будет ли проблем из-за форка, может быть стоит сабмитить в таскпул
    private void separateLemmas() {
        HtmlSeparatorServiceImpl htmlSeparatorService = new HtmlSeparatorServiceImpl();
        htmlSeparatorService.setSite(site);
        taskPool.submit(htmlSeparatorService);
    }

    private void forkURLs(Page page, long countBackslash, HashMap<String, HtmlParserServiceImpl> subtasks) {
        if (page.getResponseCode()==200) {
            extractLinks(subtasks, countBackslash, page);
            subtasks.forEach((k, v) -> {
                if (!tasksInWork.contains(k)) {
                    v.fork();
//                    taskPool.execute(v);
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
                subTasks.put(urlLink, new HtmlParserServiceImpl(site, new Page(urlLink, 418), result));
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
            Thread.sleep(ThreadLocalRandom.current().nextLong(501, 4000));
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

    private void collectResults(HashMap<String, HtmlParserServiceImpl> subTasks) {
        try {
            subTasks.forEach((k, v) -> v.join());
        } catch (Exception e) {
            logger.warn("Ошибка при Join " + e.getMessage());
        }
    }
}