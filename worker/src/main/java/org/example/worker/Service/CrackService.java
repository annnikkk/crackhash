package org.example.worker.Service;

import org.example.worker.Model.CompleteCrackRequest;
import org.example.worker.Model.WorkerRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;

@Slf4j
@Service
public class CrackService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final RestTemplate restTemplate;

    private String managerApiUrl = "http://manager:8080";

    public CrackService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void logManagerUrl() {
        log.info("Manager API URL for callbacks: {}", managerApiUrl);
    }

    // Публичный метод, который вызывает контроллер
    public void startCracking() {
        log.info("Worker started");
        while (true) {

            WorkerRequest task = getTaskFromManager();

            if (task == null) {
                log.info("Нет задач, воркер завершает работу");
                break;
            }

            log.info("Получена задача: from={} to={}", task.getFrom(), task.getTo());

            crackTask(task);
            log.info("задачку сделал");
        }
    }

    private WorkerRequest getTaskFromManager() {
        String url = managerApiUrl + "/api/hash/internal/task";

        try {
            return restTemplate.getForObject(url, WorkerRequest.class);
        } catch (Exception e) {
            log.error("Ошибка получения задачи: {}", e.getMessage());
            return null;
        }
    }

    private void crackTask(WorkerRequest req) {

        long total = (long) Math.pow(ALPHABET.length(), req.getMaxLength());

        for (long i = req.getFrom(); i <= req.getTo() && i < total; i++) {

            String word = indexToWord(i, req.getMaxLength());
            String hash = md5(word);

            if (hash.equalsIgnoreCase(req.getHash())) {
                log.info("Найдено слово: {}", word);
                sendResultToManager(req.getRequestId(), word);
                return;
            }
        }
    }

    private String indexToWord(long index, int maxLength) {
        StringBuilder sb = new StringBuilder();
        int base = ALPHABET.length();

        while (index > 0) {
            int r = (int) (index % base);
            sb.append(ALPHABET.charAt(r));
            index /= base;
        }

        while (sb.length() < maxLength) {
            sb.append(ALPHABET.charAt(0));
        }

        return sb.reverse().toString();
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendResultToManager(String requestId, String foundWord) {
        String url = managerApiUrl + "/api/hash/crack/complete";
        log.info("Sending result to manager: url={}, requestId={}, word={}", url, requestId, foundWord);
        CompleteCrackRequest body = new CompleteCrackRequest();
        body.setRequestId(requestId);
        body.setData(foundWord);
        try {
            restTemplate.postForObject(url, body, Void.class);
            log.info("Successfully sent result to manager for requestId={}", requestId);
        } catch (Exception e) {
            log.error("Failed to send result to manager: requestId={}, url={}, error={}", requestId, url, e.getMessage(), e);
        }
    }
}