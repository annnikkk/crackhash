package org.example.manager.Storage;

import org.example.manager.Model.RequestStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RequestStorage {

    private final Map<String, RequestStatus> requests = new ConcurrentHashMap<>();

    public void createRequest(String requestId, String hash, int maxLength) {
        RequestStatus status = new RequestStatus("IN_PROGRESS");
        status.setCreatedAt(System.currentTimeMillis());
        requests.put(requestId, status);
    }

    public RequestStatus getRequest(String requestId) {
        return requests.get(requestId);
    }

    public void completeRequest(String requestId, String result) {
        RequestStatus status = requests.get(requestId);
        if (status != null && "IN_PROGRESS".equals(status.getStatus())) {
            status.setStatus("READY");
            status.setData(result);
        }
    }
}