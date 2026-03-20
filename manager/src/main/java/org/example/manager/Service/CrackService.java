package org.example.manager.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.manager.Model.RequestStatus;
import org.example.manager.Model.WorkerRequest;
import org.example.manager.Storage.RequestStorage;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class CrackService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private final RequestStorage storage;
    private final ConcurrentLinkedQueue<WorkerRequest> taskQueue = new ConcurrentLinkedQueue<>();
    RestTemplate restTemplate;

    public CrackService(RequestStorage storage, RestTemplate restTemplate) {
        this.storage = storage;
        this.restTemplate = restTemplate;
    }

    public String createRequest(String hash, int maxLength) {
        String requestId = UUID.randomUUID().toString();
        storage.createRequest(requestId, hash, maxLength);
        log.info("Request created: id={}", requestId);

        splitTasks(requestId, hash, maxLength);


        new Thread(() -> {
            restTemplate.postForEntity(
                "http://worker1:8080" + "/internal/start",
                null,
                Void.class
            );
        }).start();

        new Thread(() -> {
            restTemplate.postForEntity(
                "http://worker2:8080" + "/internal/start",
                null,
                Void.class
            );
        }).start();

        new Thread(() -> {
            restTemplate.postForEntity(
                "http://worker3:8080" + "/internal/start",
                null,
                Void.class
            );
        }).start();

        return requestId;
    }

    private void splitTasks(String requestId, String hash, int maxLength) {

        int taskCount = 100;
        long total = (long) Math.pow(ALPHABET.length(), maxLength);
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
}