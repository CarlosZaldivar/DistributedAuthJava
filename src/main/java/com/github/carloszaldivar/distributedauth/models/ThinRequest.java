package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ThinRequest {
    final private String senderId;
    final private String hash;
    final private Map<String, Long> syncTimes;
    final private long timestamp;

    @JsonCreator
    public ThinRequest(@JsonProperty("senderId") String senderId, @JsonProperty("hash") String hash, @JsonProperty("syncTimes") Map<String, Long> syncTimes,
                       @JsonProperty("timestamp") long timestamp) {
        this.senderId = senderId;
        this.hash = hash;
        this.syncTimes = syncTimes;
        this.timestamp = timestamp;
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

    public long getTimestamp() {
        return timestamp;
    }
}
