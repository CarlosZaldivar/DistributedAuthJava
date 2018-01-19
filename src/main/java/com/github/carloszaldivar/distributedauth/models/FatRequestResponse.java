package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class FatRequestResponse {
    @JsonProperty("SenderId")
    final private String senderId;
    @JsonProperty("SynchroTimestamp")
    final private long syncTimestamp;
    @JsonProperty("Type")
    final private Status status;
    @JsonProperty("SynchroTimes")
    final private Map<String, Long> syncTimes;
    @JsonProperty("RequestTimestamp")
    final private long requestTimestamp;

    @JsonCreator
    public FatRequestResponse(@JsonProperty("SenderId") String senderId, @JsonProperty("Type") Status status, @JsonProperty("SynchroTimes") Map<String, Long> syncTimes,
                              @JsonProperty("RequestTimestamp") long requestTimestamp, @JsonProperty("SynchroTimestamp") long syncTimestamp) {
        this.senderId = senderId;
        this.status = status;
        this.syncTimes = syncTimes;
        this.requestTimestamp = requestTimestamp;
        this.syncTimestamp = syncTimestamp;
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, Long> getSyncTimes() {
        return syncTimes;
    }

    public long getRequestTimestamp() {
        return requestTimestamp;
    }

    public enum Status {
        OK,
        ALREADY_SYNC,
        CONFLICT,
        FIXED,
        U2OLD
    }
}
