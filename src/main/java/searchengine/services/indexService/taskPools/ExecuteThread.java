package searchengine.services.indexService.taskPools;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class ExecuteThread extends Thread {

    Task task;

    @Override
    public void run() {
        task.getTaskPool().submit(task);
        super.run();
    }
}
