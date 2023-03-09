package searchengine.services.indexService.taskPools;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RecursiveAction;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class Task extends RecursiveAction {
    private TaskPool taskPool;
    private ExecutorService executorService;
    private ExecuteThread executeThread;

    protected void stopThread() throws InterruptedException {
        executeThread.interrupt();
        executeThread.join();
    }
}
