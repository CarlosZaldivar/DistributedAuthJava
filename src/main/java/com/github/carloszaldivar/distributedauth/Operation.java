package com.github.carloszaldivar.distributedauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public enum Type { ADDING_CLIENT }

    public static String calculateHash(Operation newOperation, Operation previousOperation) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
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
