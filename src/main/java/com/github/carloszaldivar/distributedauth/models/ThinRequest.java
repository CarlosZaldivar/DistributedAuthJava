package com.github.carloszaldivar.distributedauth.models;

import java.util.Map;

public class ThinRequest {
    private String senderId;
    private String hash;
    private Map<String, Object> syncTimes;

    public ThinRequest() {
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Map<String, Object> getSyncTimes() {
        return syncTimes;
    }

    public void setSyncTimes(Map<String, Object> syncTimes) {
        this.syncTimes = syncTimes;
    }
}
