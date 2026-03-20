package org.example.manager.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestStatus {
    private String status;
    private String data;
    private long createdAt;

    public RequestStatus(String status) {
        this.status = status;
    }
}