package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ThinRequestResponse {
    @JsonProperty("Type")
    final private Status status;
    @JsonProperty("SynchroTimes")
    final private Map<String, Long> syncTimes;
    @JsonProperty("SynchroTimestamp")
    final private long timestamp;

    @JsonCreator
    public ThinRequestResponse(@JsonProperty("Type") Status status, @JsonProperty("SynchroTimes") Map<String, Long> syncTimes,
                               @JsonProperty("SynchroTimestamp") long timestamp) {
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

    public enum Status { ALREADY_SYNC, NEED_SYNC }

    public long getTimestamp() {
        return timestamp;
    }
}
