package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class Operation {
    private String hash;
    private long timestamp;
    private Type type;

    // JSON-like dictionary
    private Map<String, Object> data;

    public Operation() {
    }

    public Operation(long timestamp, Type type) {
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public boolean isAfter(Operation previousOperation) throws JsonProcessingException {
        Operation currentOperationWithoutHash = new Operation();
        currentOperationWithoutHash.setTimestamp(this.getTimestamp());
        currentOperationWithoutHash.setType(this.getType());
        currentOperationWithoutHash.setData(this.getData());

        String hash = calculateHash(currentOperationWithoutHash, previousOperation);
        return hash.equals(this.getHash());
    }

    public boolean isBefore(Operation nextOperation) throws JsonProcessingException {
        Operation nextOperationWithoutHash = new Operation();
        nextOperationWithoutHash.setTimestamp(nextOperation.getTimestamp());
        nextOperationWithoutHash.setType(nextOperation.getType());
        nextOperationWithoutHash.setData(nextOperation.getData());

        String hash = calculateHash(nextOperationWithoutHash, this);
        return hash.equals(nextOperation.getHash());
    }

    public enum Type { ADDING_CLIENT }

    public static String calculateHash(Operation newOperation, Operation previousOperation) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        String prehashString = mapper.writeValueAsString(newOperation) +
                (previousOperation == null ? "" : previousOperation.getHash());

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
