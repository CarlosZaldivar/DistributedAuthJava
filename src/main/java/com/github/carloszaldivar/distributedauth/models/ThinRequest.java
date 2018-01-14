package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ThinRequest {
    private String senderId;
    private String hash;
    private Map<String, Long> syncTimes;

    @JsonCreator
    public ThinRequest(@JsonProperty("senderId") String senderId, @JsonProperty("hash") String hash, @JsonProperty("syncTimes") Map<String, Long> syncTimes) {
        this.senderId = senderId;
        this.hash = hash;
        this.syncTimes = syncTimes;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getHash() {
        return hash;
    }

    public Map<String, Long> getSyncTimes() {
        return syncTimes;
    }
}
