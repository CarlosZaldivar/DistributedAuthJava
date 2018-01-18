package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class OneTimePasswordList {
    final private List<String> passwords;
    private int currentIndex;

    public static final int PASSWORDS_PER_LIST = 10;
    public static final int PASSWORDS_LENGTH = 10;

    public OneTimePasswordList(List<String> passwords) {
        if (passwords.size() != PASSWORDS_PER_LIST) {
            throw new IllegalArgumentException(String.format("There should be %s passwords provided.", PASSWORDS_PER_LIST));
        }
        if (!passwords.stream().allMatch(p -> p.length() == PASSWORDS_LENGTH)) {
            throw new IllegalArgumentException("All passwords should have length " + PASSWORDS_LENGTH);
        }
        this.passwords = passwords;
        this.currentIndex = 0;
    }

    @JsonCreator
    public OneTimePasswordList(@JsonProperty("passwords") List<String> passwords, @JsonProperty("currentIndex") int currentIndex) {
        this(passwords);
        this.currentIndex = currentIndex;
    }

    public OneTimePasswordList(OneTimePasswordList otherPasswordList) {
        this.passwords = new ArrayList<>(otherPasswordList.passwords);
        this.currentIndex = otherPasswordList.currentIndex;
    }

    public List<String> getPasswords() {
        return passwords;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void usePassword() {
        if (currentIndex == PASSWORDS_PER_LIST) {
            throw new RuntimeException("All passwords already used.");
        }
        ++currentIndex;
    }
}
