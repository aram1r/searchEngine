package searchengine.services.indexService.htmlParserService;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.indexService.taskPools.Task;
import searchengine.services.indexService.htmlSeparatorService.SeparationLemmaTaskImpl;
import searchengine.services.indexService.taskPools.ExecuteThread;
import searchengine.services.indexService.taskPools.TaskPool;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;


//TODO проверить как работает join, скорей всего он собирает все результаты и не требуется проверка совпадения количества задач
@Getter
@Setter
@NoArgsConstructor
@Component
public class ParserTaskImpl extends Task {

    private ConcurrentHashMap<String, Page> result;

    private static AtomicBoolean finished;

    private ConcurrentHashMap<String, ParserTaskImpl> tasksInWork;
    private Long timestamp;

    public ParserTaskImpl(Site site, TaskPool taskPool) {
        super(site, taskPool);
        getPage().setPath("/");
        result = new ConcurrentHashMap<>();
        finished = new AtomicBoolean(false);
        timestamp = System.currentTimeMillis();
        tasksInWork = new ConcurrentHashMap<>();
        tasksInWork.put("/", this);
    }


    public ParserTaskImpl(Site site, Page page, ConcurrentHashMap<String, Page> result, TaskPool taskPool,
                          ConcurrentHashMap<String, ParserTaskImpl> tasksInWork, Long timestamp) {
        super(site, taskPool, page);
        this.result = result;
        this.tasksInWork = tasksInWork;
        this.timestamp = timestamp;
    }

    @Override
    protected void compute() {
        if (!getExecutorService().isShutdown()) {
            if (!result.containsKey(getPage().getPath())) {
                processPage(getPage().getPath());
                getPage().setPath(getPage().getPath().replace(getSite().getUrl(), "/"));
                long countBackslash = getPage().getPath().chars().filter(ch -> ch == '/').count();
                result.put(getPage().getPath(), getPage());
                HashMap<String, ParserTaskImpl> subTasks = new HashMap<>();
                forkURLs(getPage(), countBackslash, subTasks);
                //TODO Переписать замер времени на АОП и логгирование
            }
            if (taskIsFinished(tasksInWork.size())) {
                collectResults();
            }
        } else {
            if (!finished.get()) {
                finished.set(true);
                collectResults();
                System.out.println("Парсинг остановлен");
            }
        }
    }

    private void separateLemmas() {
        SeparationLemmaTaskImpl htmlSeparatorService = new SeparationLemmaTaskImpl(getSite(), new TaskPool());
        htmlSeparatorService.fork();
        getExecutorService().submit(new ExecuteThread(htmlSeparatorService));
    }

    private void forkURLs(Page page, long countBackslash, HashMap<String, ParserTaskImpl> subtasks) {
        if (page.getResponseCode()==200) {
            extractLinks(subtasks, countBackslash, page);
            subtasks.forEach((k, v) -> {
                if (!tasksInWork.containsKey(k)) {
                    v.fork();
                    tasksInWork.put(k, v);
                }
            });
        }
    }

    private void extractLinks(HashMap<String, ParserTaskImpl> subTasks, long countBackslash, Page page) {
        Document document = Jsoup.parse(page.getContent(), getSite().getUrl());
        Elements links = document.select("a[href]");
        for (Element element : links) {
            String urlLink = element.absUrl("href");
            if (urlLink.contains(getSite().getUrl()) && validUrl(subTasks, countBackslash, urlLink)) {
                urlLink = urlLink.replace(getSite().getUrl(), "");
                subTasks.put(urlLink, new ParserTaskImpl(getSite(), new Page(urlLink, 418), result, getTaskPool(),
                        tasksInWork, timestamp));
            }
        }
    }

    private boolean validUrl(HashMap<String, ParserTaskImpl> subTasks, long countBackslash, String urlLink) {
        if (urlLink.chars().filter(ch -> ch == '/').count()>= countBackslash) {
            Page page = new Page(urlLink.replace(getSite().getUrl(), ""));
            page.setSite(getSite());
            if (!result.containsKey(page.getPath()) && !subTasks.containsKey(urlLink)) {
                return urlLink.endsWith("/") || urlLink.endsWith("html");
            }
        }
        return false;
    }

    private void processPage (String url) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(501, 4000));
            Connection.Response response = Jsoup.connect(getSite().getUrl()+url).userAgent(getAppProps().getUserAgent())
                    .referrer(getAppProps().getReferrer()).execute();
            getPage().setSite(getSite());
            getPage().setResponseCode(response.statusCode());
            if (getPage().getResponseCode()==200) {
                getPage().setContent(response.parse().toString());
            } else {
                getPage().setContent("");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void collectResults() {
        try {
            //TODO поправить join
            tasksInWork.remove("/");
            System.out.println("Parsing " + getSite().getName() +" took " + (System.currentTimeMillis()-timestamp)/60000 + " minutes");
            getPageRepository().saveAll(result.values());
            //Начинаем обработку лемм сайта
            if (!getExecutorService().isShutdown()) {
                separateLemmas();
            } else {
                getExecutorService().shutdownNow();
            }
            getTaskPool().shutdownNow();
        } catch (Exception e) {
            getLogger().warn("Ошибка при Join " + e.getMessage());
            System.out.println("Error during results collecting");
        }
    }

    private synchronized boolean taskIsFinished(Integer integer) {
        return result.size() == integer;
    }
}