package org.example.manager.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskInfo {
    private WorkerRequest task;
    private long timeout;

    public TaskInfo(long timeout, WorkerRequest task) {
        this.timeout = timeout;
        this.task = task;
    }
}
