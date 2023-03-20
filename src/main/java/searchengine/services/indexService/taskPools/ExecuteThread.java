package searchengine.services.indexService.taskPools;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class ExecuteThread extends Thread {

//    Task task;
//
//    @Override
//    public void run() {
//        task.setExecuteThread(this);
//        task.getTaskPool().submit(task);
//        super.run();
//    }

    Task htmlService;

    @Override
    public void run() {
        htmlService.setExecuteThread(this);
        htmlService.getTaskPool().submit(htmlService);
        super.run();
    }
}
