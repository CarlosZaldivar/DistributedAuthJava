package com.github.carloszaldivar.distributedauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DistributedAuthApplicationTests {
    private Operation addingClientOperation;

    @Before
    public void prepareTests() throws JsonProcessingException {
        addingClientOperation = new Operation();
        addingClientOperation.setType(Operation.Type.ADDING_CLIENT);
        addingClientOperation.setTimestamp(System.currentTimeMillis());
        Map<String, Object> data = new HashMap<>();
        data.put("name", "123456");
        data.put("pin", "1234");
        addingClientOperation.setData(data);
        addingClientOperation.setHash(Operation.calculateHash(addingClientOperation, null));
    }

	@Test
	public void addingClientTest() throws JsonProcessingException {
        Assert.assertEquals(Clients.get().size(), 0);
        Assert.assertEquals(Operations.get().size(), 0);
	    ClientsController controller = new ClientsController();
	    Client client = new Client();
	    client.setNumber("123456");
	    client.setPin("1234");

	    controller.create(client);

	    Assert.assertEquals(Clients.get().size(), 1);
        Assert.assertEquals(Clients.get().get("123456"), client);
        Assert.assertEquals(Operations.get().size(), 1);
        Assert.assertEquals(Operations.get().get(0).getType(), Operation.Type.ADDING_CLIENT);
	}

	@Test
    public void addingNeighbourTest() {
        Assert.assertEquals(Neighbours.get().size(), 0);
        NeighboursController controller = new NeighboursController();
        Neighbour neighbour = new Neighbour("Server1", "localhost:10000");

        controller.addNeighbour(neighbour);

        Assert.assertEquals(Neighbours.get().size(), 1);
        Assert.assertEquals(Neighbours.get().get(0), neighbour);
    }
}
