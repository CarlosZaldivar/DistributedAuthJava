package com.github.carloszaldivar.distributedauth.models;

import java.util.Map;

public class FatRequestResponse {
    private Status status;
    private Map<String, Long> syncTimes;

    public FatRequestResponse() { }

    public FatRequestResponse(Status status) {
        this.status = status;
    }

    public FatRequestResponse(Status status, Map<String, Long> syncTimes) {
        this.status = status;
        this.syncTimes = syncTimes;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, Long> getSyncTimes() {
        return syncTimes;
    }

    public void setSyncTimes(Map<String, Long> syncTimes) {
        this.syncTimes = syncTimes;
    }

    public enum Status { OK, CONFLICT, U2OLD }
}
