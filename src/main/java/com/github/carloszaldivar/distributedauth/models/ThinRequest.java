package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ThinRequest {
    @JsonProperty("SenderId")
    final private String senderId;
    @JsonProperty("LastHash")
    final private String hash;
    @JsonProperty("SynchroTimestamp")
    final private Map<String, Long> syncTimes;
    @JsonProperty("Timestamp")
    final private long timestamp;

    @JsonCreator
    public ThinRequest(@JsonProperty("SenderId") String senderId, @JsonProperty("LastHash") String hash,
                       @JsonProperty("SynchroTimestamp") Map<String, Long> syncTimes,
                       @JsonProperty("Timestamp") long timestamp) {
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
