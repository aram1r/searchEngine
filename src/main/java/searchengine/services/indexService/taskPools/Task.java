package searchengine.services.indexService.taskPools;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.configuration.AppProps;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RecursiveAction;

@Getter
@Setter
@Component
@AllArgsConstructor
@NoArgsConstructor
public abstract class Task extends RecursiveAction {
    private Site site;
    private Page page;

    private TaskPool taskPool;
    private static PageRepository pageRepository;
    private static SiteRepository siteRepository;
    private static Logger logger;
    private static ExecutorService executorService;
    private static AppProps appProps;
    private ExecuteThread executeThread;

    @Autowired
    public final void setExecutorService(ExecutorService executorService) {
        Task.executorService = executorService;
    }

    @Autowired
    public final void setPageRepository(PageRepository pageRepository) {
        Task.pageRepository = pageRepository;
    }

    @Autowired
    public final void setSiteRepository(SiteRepository siteRepository) {
        Task.siteRepository = siteRepository;
    }

    @Autowired
    public final void setAppProps(AppProps appProps) {
        Task.appProps = appProps;
    }

    @Autowired
    public void setLogger() {
        logger = LoggerFactory.getLogger("HtmlParser");
    }

    @Override
    protected void compute() {

    }

    public PageRepository getPageRepository() {
        return pageRepository;
    }

    public SiteRepository getSiteRepository() {
        return siteRepository;
    }

    public ExecutorService getExecutorService() {return executorService;}

    public Logger getLogger() {
        return logger;
    }

    public static AppProps getAppProps() {
        return appProps;
    }

    public Task(Site site, TaskPool taskPool) {
        this.taskPool = taskPool;
        this.site = site;
        this.page = new Page();
    }

    public Task(Site site, TaskPool taskPool, Page page) {
        this(site, taskPool);
        this.page = page;
    }

    protected void stopThread() throws InterruptedException {
        executeThread.interrupt();
        executeThread.join();
    }
}
