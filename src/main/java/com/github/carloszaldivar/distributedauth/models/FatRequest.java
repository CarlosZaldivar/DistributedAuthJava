package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class FatRequest {
    @JsonProperty("SenderId")
    final private String senderId;
    @JsonProperty("History")
    final private List<Operation> history;
    @JsonProperty("SynchroTimes")
    final private Map<String, Long> syncTimes;
    @JsonProperty("RequestTimestamp")
    final private long timestamp;

    @JsonCreator
    public FatRequest(@JsonProperty("SenderId") String senderId, @JsonProperty("History") List<Operation> history,
                      @JsonProperty("SynchroTimes") Map<String, Long> syncTimes, @JsonProperty("RequestTimestamp") long timestamp) {
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
