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
        Assert.assertEquals("6c8a259ddcfdb14dcf08ed0dac1313de982af7320e865fb306021086dbe323bf", firstOperation.getHash());
        Assert.assertEquals("8d353dafd19a95e4b763fc9fda39dcab8b086122ac54efc0f5df73537518f728", secondOperation.getHash());
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
