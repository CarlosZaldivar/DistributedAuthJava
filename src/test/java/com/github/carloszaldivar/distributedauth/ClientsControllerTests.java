package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.controllers.ClientsController;
import com.github.carloszaldivar.distributedauth.data.Clients;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.data.Operations;
import com.github.carloszaldivar.distributedauth.models.AuthorizationRequest;
import com.github.carloszaldivar.distributedauth.models.Client;
import com.github.carloszaldivar.distributedauth.models.Operation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static com.github.carloszaldivar.distributedauth.models.OneTimePasswordList.PASSWORDS_PER_LIST;

public class ClientsControllerTests {

    @Test
    public void addingClientTest() {
        Assert.assertEquals(0, Clients.get().size());
        Assert.assertEquals(0, Operations.get().size());
        ClientsController controller = new ClientsController();
        Client client = new Client("123456", "1234", null, null);

        controller.create(client);

        Assert.assertEquals(1, Clients.get().size());
        Assert.assertEquals(client, Clients.get().get("123456"));
        Assert.assertEquals(1, Operations.get().size());
        Assert.assertEquals(Operation.Type.ADDING_CLIENT, Operations.get().get(0).getType());
    }

    @Test
    public void deletingClientTest() {
        Assert.assertEquals(0, Clients.get().size());
        Assert.assertEquals(0, Operations.get().size());
        ClientsController controller = new ClientsController();
        Client client = new Client("123456", "1234", null, null);

        controller.create(client);
        controller.delete(client.getNumber());

        Assert.assertEquals(0, Clients.get().size());
    }

    @Test
    public void authenticateTest() {
        ClientsController controller = new ClientsController();
        Client client = new Client("123456", "1234", null, null);

        controller.create(client);
        Assert.assertEquals(HttpStatus.OK, controller.authenticate(client.getNumber(), client.getPin()).getStatusCode());
    }

    @Test
    public void authorizeTest() {
        ClientsController controller = new ClientsController();
        Client client = new Client("123456", "1234", null, null);

        controller.create(client);
        for (String password : client.getActivatedList().getPasswords().subList(0, PASSWORDS_PER_LIST - 1)) {
            AuthorizationRequest request = new AuthorizationRequest(client.getPin(), password);
            Assert.assertEquals(HttpStatus.OK,
                    controller.authorizeOperation(client.getNumber(), request).getStatusCode());
        }
    }

    @Test
    public void activateNewListTest() {
        ClientsController controller = new ClientsController();
        Client client = new Client("123456", "1234", null, null);

        controller.create(client);
        List<String> passwords = client.getActivatedList().getPasswords();
        for (String password : passwords.subList(0, PASSWORDS_PER_LIST - 1)) {
            AuthorizationRequest request = new AuthorizationRequest(client.getPin(), password);
            controller.authorizeOperation(client.getNumber(), request);
        }
        AuthorizationRequest request = new AuthorizationRequest(client.getPin(), passwords.get(PASSWORDS_PER_LIST - 1));
        Assert.assertEquals(HttpStatus.OK, controller.activateNewPasswordList(client.getNumber(), request).getStatusCode());
    }

    @After
    public void cleanGlobalData() {
        Clients.get().clear();
        Neighbours.get().clear();
        Neighbours.getSyncTimes().clear();
        Operations.get().clear();
        DistributedAuthApplication.setState(DistributedAuthApplication.State.SYNCHRONIZED);
    }
}
