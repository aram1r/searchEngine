package searchengine.services.indexService.taskPools;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ForkJoinPool;

@Setter
@Getter
public class URLTaskPool extends ForkJoinPool {
    boolean isIndexing;

    public URLTaskPool() {
        isIndexing=false;
    }

    public void stopIndexing() {
        this.setIndexing(false);
        this.shutdownNow();
    }
}
