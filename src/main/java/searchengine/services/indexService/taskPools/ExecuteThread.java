package searchengine.services.indexService.taskPools;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.concurrent.RecursiveAction;

@AllArgsConstructor
@Setter
@Getter
public class ExecuteThread extends Thread {

    TaskPool taskPool;

    RecursiveAction recursiveAction;

    public ExecuteThread (RecursiveAction recursiveAction) {
        taskPool = new TaskPool();
        this.recursiveAction = recursiveAction;
    }

    @Override
    public void run() {
        taskPool.submit(recursiveAction);
        super.run();
    }
}
