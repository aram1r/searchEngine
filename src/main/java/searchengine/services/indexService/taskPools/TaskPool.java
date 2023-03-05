package searchengine.services.indexService.taskPools;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ForkJoinPool;

@Setter
@Getter
public class TaskPool extends ForkJoinPool {
//    public TaskPool(int parallelism) {
//        super(parallelism);
//    }

    @Override
    public void execute(Runnable task) {
        super.execute(task);
    }


}
