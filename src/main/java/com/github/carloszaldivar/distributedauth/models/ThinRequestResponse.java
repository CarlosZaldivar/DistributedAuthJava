package com.github.carloszaldivar.distributedauth.models;

import java.util.Map;

public class ThinRequestResponse {
    private Status status;
    private Map<String, Long> syncTimes;

    public ThinRequestResponse(Status status, Map<String, Long> syncTimes) {
        this.status = status;
        this.syncTimes = syncTimes;
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, Long> getSyncTimes() {
        return syncTimes;
    }

    public enum Status { UPDATE_NOT_NEEDED, UPDATE_NEEDED }
}
