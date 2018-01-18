package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@JsonPropertyOrder({ "data", "hash", "number", "timestamp", "type" })
public class Operation {
    final private String hash;
    final private long timestamp;
    final private Type type;
    final private int number;

    final private Client clientBefore;
    final private Client clientAfter;

    @JsonCreator
    public Operation(@JsonProperty("timestamp") long timestamp,
                     @JsonProperty("type") Type type,
                     @JsonProperty("number") int number,
                     @JsonProperty("clientBefore") Client clientBefore,
                     @JsonProperty("clientAfter") Client clientAfter,
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

    public enum Type { ADDING_CLIENT, REMOVING_CLIENT }

    private String calculateHash(Operation previousOperation) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        String prehashString;
        try {
            prehashString = mapper.writeValueAsString(this) +
                    (previousOperation == null ? "" : previousOperation.getHash());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

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
