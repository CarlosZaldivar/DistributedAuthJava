package com.github.carloszaldivar.distributedauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.carloszaldivar.distributedauth.models.Operation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class OperationTests {
    private Operation firstOperation;
    private Operation secondOperation;

    public OperationTests() {
        firstOperation = new Operation();
        firstOperation.setType(Operation.Type.ADDING_CLIENT);
        firstOperation.setTimestamp(100);
        Map<String, Object> firstData = new HashMap<>();
        firstData.put("number", "123456");
        firstData.put("pin", "1234");
        firstOperation.setData(firstData);

        secondOperation = new Operation();
        secondOperation.setType(Operation.Type.ADDING_CLIENT);
        secondOperation.setTimestamp(110);
        Map<String, Object> secondData = new HashMap<>();
        secondData.put("number", "123457");
        secondData.put("pin", "1235");
        secondOperation.setData(secondData);
    }

    @Test
    public void calculateHashTest() throws JsonProcessingException {
        String firstHash = Operation.calculateHash(firstOperation, null);
        Assert.assertEquals("45156ffa28d815a1c9b9638e0cfaf1a79b086fc4964304913975cf8401273c36", firstHash);
        firstOperation.setHash(firstHash);
        String secondHash = Operation.calculateHash(secondOperation, firstOperation);
        Assert.assertEquals("db40041d03a8e7f370f99f679c61336db23bdc2e10d79f85622cb6bfb54b7aa0", secondHash);
    }

    @Test
    public void isAfterTest() throws JsonProcessingException {
        String firstHash = Operation.calculateHash(firstOperation, null);
        firstOperation.setHash(firstHash);
        String secondHash = Operation.calculateHash(secondOperation, firstOperation);
        secondOperation.setHash(secondHash);
        Assert.assertTrue(secondOperation.isAfter(firstOperation));
    }

    @Test
    public void isBeforeTest() throws JsonProcessingException {
        String firstHash = Operation.calculateHash(firstOperation, null);
        firstOperation.setHash(firstHash);
        String secondHash = Operation.calculateHash(secondOperation, firstOperation);
        secondOperation.setHash(secondHash);
        Assert.assertTrue(firstOperation.isBefore(secondOperation));
    }

    @After
    public void clearHashes() {
        firstOperation.setHash(null);
        secondOperation.setHash(null);
    }
}
