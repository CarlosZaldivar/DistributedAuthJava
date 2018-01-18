package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.controllers.ClientsController;
import com.github.carloszaldivar.distributedauth.controllers.SoftAuthorizationController;
import com.github.carloszaldivar.distributedauth.data.Clients;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.data.Operations;
import com.github.carloszaldivar.distributedauth.models.AuthorizationRequest;
import com.github.carloszaldivar.distributedauth.models.Client;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static com.github.carloszaldivar.distributedauth.models.OneTimePasswordList.PASSWORDS_PER_LIST;

public class SoftAuthorizationControllerTests {
    @Test
    public void authorizeOperationTest() {
        Client client = new Client("123456", "1234", null, null);
        ClientsController clientsController = new ClientsController();
        clientsController.create(client);
        SoftAuthorizationController softAuthorizationController = new SoftAuthorizationController();

        AuthorizationRequest request = new AuthorizationRequest(client.getPin(), client.getActivatedList().getPasswords().get(0));
        Assert.assertEquals(HttpStatus.OK, softAuthorizationController.authorizeOperation(client.getNumber(), request).getStatusCode());
    }

    @Test
    public void activateNewListTest() {
        Client client = new Client("123456", "1234", null, null);
        ClientsController clientsController = new ClientsController();
        clientsController.create(client);

        List<String> passwords = client.getActivatedList().getPasswords();
        for (String password : passwords.subList(0, PASSWORDS_PER_LIST - 1)) {
            AuthorizationRequest request = new AuthorizationRequest(client.getPin(), password);
            clientsController.authorizeOperation(client.getNumber(), request);
        }

        SoftAuthorizationController softAuthorizationController = new SoftAuthorizationController();
        AuthorizationRequest request = new AuthorizationRequest(client.getPin(), passwords.get(PASSWORDS_PER_LIST - 1));
        Assert.assertEquals(HttpStatus.OK, softAuthorizationController.activateNewPasswordList(client.getNumber(), request).getStatusCode());
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