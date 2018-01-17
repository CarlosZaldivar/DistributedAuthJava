package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthorizationRequest {
    final private String pin;
    final private String oneTimePassword;

    @JsonCreator
    public AuthorizationRequest(@JsonProperty("pin") String pin, @JsonProperty("oneTimePassword") String oneTimePassword) {
        this.pin = pin;
        this.oneTimePassword = oneTimePassword;
    }

    public String getPin() {
        return pin;
    }

    public String getOneTimePassword() {
        return oneTimePassword;
    }
}