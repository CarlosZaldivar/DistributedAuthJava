package com.github.carloszaldivar.distributedauth.models;

public class Client {
    private String number;
    private String pin;

    public Client() { }

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
}
