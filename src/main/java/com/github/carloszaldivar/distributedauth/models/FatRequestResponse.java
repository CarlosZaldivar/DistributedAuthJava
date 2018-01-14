package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class FatRequestResponse {
    private Status status;
    private Map<String, Long> syncTimes;

    @JsonCreator
    public FatRequestResponse(@JsonProperty("status") Status status, @JsonProperty("syncTimes") Map<String, Long> syncTimes) {
        this.status = status;
        this.syncTimes = syncTimes;
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, Long> getSyncTimes() {
        return syncTimes;
    }

    public enum Status { OK, CONFLICT, U2OLD }
}
