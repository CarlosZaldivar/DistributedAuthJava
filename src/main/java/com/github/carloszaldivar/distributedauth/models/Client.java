package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Client {
    private String number;
    private String pin;
    private OneTimePasswordList activatedList;
    private OneTimePasswordList nonactivatedList;

    @JsonCreator
    public Client(@JsonProperty("number") String number, @JsonProperty("pin") String pin,
                  @JsonProperty("activatedList") OneTimePasswordList activatedList,
                  @JsonProperty("nonactivatedList") OneTimePasswordList nonactivatedList) {
        this.number = number;
        this.pin = pin;
        this.activatedList = activatedList;
        this.nonactivatedList = nonactivatedList;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public OneTimePasswordList getActivatedList() {
        return activatedList;
    }

    public void setActivatedList(OneTimePasswordList activatedList) {
        this.activatedList = activatedList;
    }

    public OneTimePasswordList getNonactivatedList() {
        return nonactivatedList;
    }

    public void setNonactivatedList(OneTimePasswordList nonactivatedList) {
        this.nonactivatedList = nonactivatedList;
    }
}
