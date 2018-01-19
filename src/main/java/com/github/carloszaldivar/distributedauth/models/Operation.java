package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Operation {
    @JsonProperty("Hash")
    final private String hash;
    @JsonProperty("Timestamp")
    final private long timestamp;
    @JsonProperty("Type")
    final private Type type;
    @JsonProperty("SequenceNumber")
    final private int number;

    @JsonProperty("DataBefore")
    final private Client clientBefore;
    @JsonProperty("DataAfter")
    final private Client clientAfter;

    @JsonCreator
    public Operation(@JsonProperty("Timestamp") long timestamp,
                     @JsonProperty("Type") Type type,
                     @JsonProperty("SequenceNumber") int number,
                     @JsonProperty("DataBefore") Client clientBefore,
                     @JsonProperty("DataAfter") Client clientAfter,
                     @JsonProperty("previousOperation") Operation previousOperation) {
        this.timestamp = timestamp;
        this.type = type;
        this.number = number;
        this.clientBefore = clientBefore;
        this.clientAfter = clientAfter;
        this.hash = calculateHash(previousOperation);
    }

    public String getHash() {
        return hash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Type getType() {
        return type;
    }

    public int getNumber() {
        return number;
    }

    public Client getClientBefore() {
        return clientBefore;
    }

    public Client getClientAfter() {
        return clientAfter;
    }

    public boolean isAfter(Operation previousOperation) {
        Operation calculatedCurrentOperation = new Operation(
                this.getTimestamp(),
                this.getType(),
                this.getNumber(),
                this.getClientBefore(),
                this.getClientAfter(),
                previousOperation);
        return calculatedCurrentOperation.getHash().equals(this.getHash());
    }

    public boolean isBefore(Operation nextOperation) {
        Operation calculatedNextOperation = new Operation(
                nextOperation.getTimestamp(),
                nextOperation.getType(),
                nextOperation.getNumber(),
                nextOperation.clientBefore,
                nextOperation.clientAfter,
                this);
        return calculatedNextOperation.getHash().equals(nextOperation.getHash());
    }

    public enum Type { ADD_CLIENT, DELETE_CLIENT, AUTHORIZATION, LIST_ACTIVATION }

    private String calculateHash(Operation previousOperation) {
        ObjectMapper mapper = new ObjectMapper();

        String serializedBefore;
        try {
            serializedBefore = mapper.writeValueAsString(clientBefore);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String serializedAfter;
        try {
            serializedAfter = mapper.writeValueAsString(clientAfter);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String prehashString = String.format("%s%s%s%s%s", timestamp, type.toString(), serializedBefore, serializedAfter,
                previousOperation == null ? "" : previousOperation.getHash());
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found. Situation expected to never happen.");
        }
        byte[] hashBytes = digest.digest(prehashString.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(hashBytes);
    }
}
