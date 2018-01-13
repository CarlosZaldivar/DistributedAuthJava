package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.models.Operation;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class OperationTests {
    private Operation firstOperation;
    private Operation secondOperation;

    public OperationTests() {
        Map<String, Object> firstData = new HashMap<>();
        firstData.put("number", "123456");
        firstData.put("pin", "1234");
        firstOperation = new Operation(100, Operation.Type.ADDING_CLIENT, 0, firstData, null);

        Map<String, Object> secondData = new HashMap<>();
        secondData.put("number", "123457");
        secondData.put("pin", "1235");
        secondOperation = new Operation(100, Operation.Type.ADDING_CLIENT, 1, secondData, firstOperation);
    }

    @Test
    public void calculateHashTest() {
        Assert.assertEquals("9548d50bf82d7d29a220fe5923798bd494f2ff9f60e735825f6d09ccc317d995", firstOperation.getHash());
        Assert.assertEquals("a44b99bb6206ea2e45fe442819e30e37f521d99a98ed9a8319a4246214627b2d", secondOperation.getHash());
    }

    @Test
    public void isAfterTest() {
        Assert.assertTrue(secondOperation.isAfter(firstOperation));
    }

    @Test
    public void isBeforeTest() {
        Assert.assertTrue(firstOperation.isBefore(secondOperation));
    }
}
