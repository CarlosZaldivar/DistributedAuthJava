package com.github.carloszaldivar.distributedauth.models;

import java.util.List;
import java.util.Map;

public class FatRequest {

    private String senderId;
    private List<Operation> history;
    private Map<String, Long> syncTimes;

    public List<Operation> getHistory() {
        return history;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setHistory(List<Operation> history) {
        this.history = history;
    }

    public Map<String, Long> getSyncTimes() {
        return syncTimes;
    }

    public void setSyncTimes(Map<String, Long> syncTimes) {
        this.syncTimes = syncTimes;
    }
}
