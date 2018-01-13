package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.controllers.ClientsController;
import com.github.carloszaldivar.distributedauth.data.Clients;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.data.Operations;
import com.github.carloszaldivar.distributedauth.models.Client;
import com.github.carloszaldivar.distributedauth.models.Operation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class ClientsControllerTests {

    @Test
    public void addingClientTest() {
        Assert.assertEquals(0, Clients.get().size());
        Assert.assertEquals(0, Operations.get().size());
        ClientsController controller = new ClientsController();
        Client client = new Client();
        client.setNumber("123456");
        client.setPin("1234");

        controller.create(client);

        Assert.assertEquals(1, Clients.get().size());
        Assert.assertEquals(client, Clients.get().get("123456"));
        Assert.assertEquals(1, Operations.get().size());
        Assert.assertEquals(Operation.Type.ADDING_CLIENT, Operations.get().get(0).getType());
    }

    @After
    public void cleanSingletons() {
        Clients.get().clear();
        Neighbours.get().clear();
        Neighbours.getSyncTimes().clear();
        Operations.get().clear();
    }
}
