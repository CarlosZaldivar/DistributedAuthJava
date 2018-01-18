package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.models.Client;
import com.github.carloszaldivar.distributedauth.models.Operation;
import org.junit.Assert;
import org.junit.Test;

public class OperationTests {
    private Operation firstOperation;
    private Operation secondOperation;

    public OperationTests() {
        Client client1 = new Client("123456", "1234", null, null);
        firstOperation = new Operation(100, Operation.Type.ADDING_CLIENT, 0, null, client1, null);

        Client client2 = new Client("123457", "1234", null, null);
        secondOperation = new Operation(100, Operation.Type.ADDING_CLIENT, 1, null, client2, firstOperation);
    }

    @Test
    public void calculateHashTest() {
        Assert.assertEquals("2fe5bdddb61055232b39e799e544e2737e1f4d0490634a16e2005d27fd9accfb", firstOperation.getHash());
        Assert.assertEquals("f25feb421634285a3d11e5993aa935e2412ec52718ac505453413c1f7efc38f4", secondOperation.getHash());
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
