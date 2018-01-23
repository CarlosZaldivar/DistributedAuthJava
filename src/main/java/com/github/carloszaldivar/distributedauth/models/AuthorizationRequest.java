package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthorizationRequest {
    @JsonProperty("Pin")
    final private String pin;
    @JsonProperty("OneTimePassword")
    final private String oneTimePassword;

    @JsonCreator
    public AuthorizationRequest(@JsonProperty("Pin") String pin, @JsonProperty("OneTimePassword") String oneTimePassword) {
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