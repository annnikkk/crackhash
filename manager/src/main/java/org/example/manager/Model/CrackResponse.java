package org.example.manager.Model;

public class CrackResponse {
    
    private String requestId;

    public CrackResponse (String requestId){
        this.requestId = requestId;
    }
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
