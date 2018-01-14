package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class FatRequest {

    private String senderId;
    private List<Operation> history;
    private Map<String, Long> syncTimes;

    @JsonCreator
    public FatRequest(@JsonProperty("senderId") String senderId, @JsonProperty("history") List<Operation> history,
                      @JsonProperty("syncTimes") Map<String, Long> syncTimes) {
        this.senderId = senderId;
        this.history = history;
        this.syncTimes = syncTimes;
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
}
