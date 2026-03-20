package org.example.manager.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkerRequest {
    private String requestId;
    private String hash;
    private int maxLength;
    private long from;
    private long to;
}