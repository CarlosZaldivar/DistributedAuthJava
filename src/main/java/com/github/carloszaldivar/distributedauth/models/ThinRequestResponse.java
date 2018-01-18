package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ThinRequestResponse {
    final private Status status;
    final private Map<String, Long> syncTimes;
    final private long timestamp;

    @JsonCreator
    public ThinRequestResponse(@JsonProperty("status") Status status, @JsonProperty("syncTimes") Map<String, Long> syncTimes,
                               @JsonProperty("timestamp") long timestamp) {
        this.status = status;
        this.syncTimes = syncTimes;
        this.timestamp = timestamp;
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, Long> getSyncTimes() {
        return syncTimes;
    }

    public enum Status { UPDATE_NOT_NEEDED, UPDATE_NEEDED }

    public long getTimestamp() {
        return timestamp;
    }
}
