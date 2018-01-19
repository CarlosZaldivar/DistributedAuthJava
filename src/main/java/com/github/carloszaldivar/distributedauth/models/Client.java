package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;

import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({"Id", "Pin", "ActivatedList", "NonactivatedList"})
public class Client {
    @JsonProperty("Id")
    private String number;
    @JsonProperty("Pin")
    private String pin;
    @JsonProperty("ActivatedList")
    private OneTimePasswordList activatedList;
    @JsonProperty("NonactivatedList")
    private OneTimePasswordList nonactivatedList;

    @JsonCreator
    public Client(@JsonProperty("Id") String number, @JsonProperty("Pin") String pin,
                  @JsonProperty("ActivatedList") OneTimePasswordList activatedList,
                  @JsonProperty("NonactivatedList") OneTimePasswordList nonactivatedList) {
        this.number = number;
        this.pin = pin;
        this.activatedList = activatedList;
        this.nonactivatedList = nonactivatedList;
    }

    public Client(Client otherClient) {
        this.number = otherClient.number;
        this.pin = otherClient.pin;
        this.activatedList = new OneTimePasswordList(otherClient.getActivatedList());
        this.nonactivatedList = new OneTimePasswordList(otherClient.getNonactivatedList());
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

    public OneTimePasswordList getNonactivatedList() {
        return nonactivatedList;
    }

    public void generateOneTimePasswordLists() {
        this.activatedList = generateOneTimePasswordList();
        this.nonactivatedList = generateOneTimePasswordList();
    }

    public boolean useOneTimePassword(String password) {
        int currentIndex = activatedList.getCurrentIndex();
        if (currentIndex == OneTimePasswordList.PASSWORDS_PER_LIST - 1) {
            throw new RuntimeException("Last password has to be used to activate next password list.");
        }
        boolean isAuthorized = password.equals(activatedList.getPasswords().get(currentIndex));
        if (isAuthorized) {
            activatedList.usePassword();
        }
        return isAuthorized;
    }

    public boolean activateNewOneTimePasswordList(String password) {
        int currentIndex = activatedList.getCurrentIndex();
        if (currentIndex != OneTimePasswordList.PASSWORDS_PER_LIST - 1) {
            throw new RuntimeException(("Only last password can be used to activate the new list."));
        }
        boolean isActivated = activatedList.getPasswords().get(currentIndex).equals(password);
        if (isActivated) {
            activatedList = nonactivatedList;
            nonactivatedList = generateOneTimePasswordList();
        }
        return isActivated;
    }

    private OneTimePasswordList generateOneTimePasswordList() {
        RandomStringGenerator randomStringGenerator =
                new RandomStringGenerator.Builder()
                        .withinRange('0', 'z')
                        .filteredBy(CharacterPredicates.LETTERS, CharacterPredicates.DIGITS)
                        .build();
        List<String> passwords = new ArrayList<>();
        for (int i = 0; i < OneTimePasswordList.PASSWORDS_PER_LIST; ++i) {
            passwords.add(randomStringGenerator.generate(OneTimePasswordList.PASSWORDS_LENGTH));
        }
        return new OneTimePasswordList(passwords);
    }
}
