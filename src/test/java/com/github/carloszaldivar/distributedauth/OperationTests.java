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
        firstOperation = new Operation(100, Operation.Type.ADD_CLIENT, 0, null, client1, null);

        Client client2 = new Client("123457", "1234", null, null);
        secondOperation = new Operation(100, Operation.Type.ADD_CLIENT, 1, null, client2, firstOperation);
    }

    @Test
    public void calculateHashTest() {
        Assert.assertEquals("c16ec90336d13dd7771fced56b086962eafdce6f6b6861acb9ee72a09645e80c", firstOperation.getHash());
        Assert.assertEquals("a73fcd6f25b91f74351eb9496b7670476c7568fa0462f81e32bcb2208aee0dfd", secondOperation.getHash());
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
