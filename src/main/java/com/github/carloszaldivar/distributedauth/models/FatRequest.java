package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class FatRequest {
    final private String senderId;
    final private List<Operation> history;
    final private Map<String, Long> syncTimes;
    final private long timestamp;

    @JsonCreator
    public FatRequest(@JsonProperty("senderId") String senderId, @JsonProperty("history") List<Operation> history,
                      @JsonProperty("syncTimes") Map<String, Long> syncTimes, @JsonProperty("timestamp") long timestamp) {
        this.senderId = senderId;
        this.history = history;
        this.syncTimes = syncTimes;
        this.timestamp = timestamp;
    }

    public List<Operation> getHistory() {
        return history;
    }

    public String getSenderId() {
        return senderId;
    }

    public Map<String, Long> getSyncTimes() {
        return syncTimes;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
