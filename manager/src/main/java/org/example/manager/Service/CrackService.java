package org.example.manager.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.manager.Model.RequestStatus;
import org.example.manager.Model.TaskInfo;
import org.example.manager.Model.WorkerRequest;
import org.example.manager.Storage.RequestStorage;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class CrackService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private final RequestStorage storage;
    private final ConcurrentLinkedQueue<WorkerRequest> taskQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, TaskInfo> workerTasks = new ConcurrentHashMap<>();
    RestTemplate restTemplate;

    public CrackService(RequestStorage storage, RestTemplate restTemplate) {
        this.storage = storage;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void initTaskMonitoring(){
        Thread t = new Thread(() -> {
            while (true) {
                checkTimeouts();
                try{
                    Thread.sleep(2000);
                } catch (InterruptedException e){
                    throw new RuntimeException();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void checkTimeouts() {
        Iterator<Map.Entry<String, TaskInfo>> iterator = workerTasks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, TaskInfo> entry = iterator.next();
            String workerNumber = entry.getKey();
            TaskInfo taskInfo = entry.getValue();

            if (System.currentTimeMillis() > taskInfo.getTimeout()) {
                log.info("Worker {} timeout!", workerNumber);
                taskQueue.add(taskInfo.getTask());
                iterator.remove();
            }
        }
    }

    public String createRequest(String hash, int maxLength) {
        String requestId = UUID.randomUUID().toString();
        storage.createRequest(requestId, hash, maxLength);
        log.info("Request created: id={}", requestId);

        splitTasks(requestId, hash, maxLength);

        return requestId;
    }

    private void splitTasks(String requestId, String hash, int maxLength) {

        int taskCount = 100;
        long total = 0;
        for (int i = 1; i <= maxLength; i++){
            total += (long) Math.pow(ALPHABET.length(), i);
        }
        long chunkSize = total / taskCount;

        log.info("Splitting tasks: total={}, chunkSize={}, tasks={}", total, chunkSize, taskCount);

        for (int i = 0; i < taskCount; i++) {
            WorkerRequest task = new WorkerRequest();

            task.setRequestId(requestId);
            task.setHash(hash);
            task.setMaxLength(maxLength);

            long from = i * chunkSize;
            long to = (i == taskCount - 1) ? total : (i + 1) * chunkSize;
            task.setFrom(from);
            task.setTo(to);

            taskQueue.add(task);

            log.info("Task created: from={} to={}", from, to);
        }
    }   

    public WorkerRequest getTask() {
        return taskQueue.poll();
    }

    public void completeRequest(String requestId, String foundWord) {
        storage.completeRequest(requestId, foundWord);
        log.info("Request {} completed with word: {}", requestId, foundWord);
    }

    public RequestStatus getStatus(String requestId) {
        RequestStatus status = storage.getRequest(requestId);

        if (status == null) return null;

        long now = System.currentTimeMillis();
        if ("IN_PROGRESS".equals(status.getStatus()) && now - status.getCreatedAt() > 30000) {
            log.warn("Request {} timeout!", requestId);
            status.setStatus("ERROR");
        }
        return status;
    }

    public void assignTask(String workerNumber, WorkerRequest task){
        long combinations = task.getTo() - task.getFrom();
        long timeout = System.currentTimeMillis() + 5000;
        workerTasks.put(workerNumber, new TaskInfo(timeout, task));
    }
}