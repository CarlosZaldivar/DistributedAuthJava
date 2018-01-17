package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.controllers.ClientsController;
import com.github.carloszaldivar.distributedauth.controllers.NeighboursController;
import com.github.carloszaldivar.distributedauth.controllers.FatRequestController;
import com.github.carloszaldivar.distributedauth.controllers.ThinRequestController;
import com.github.carloszaldivar.distributedauth.data.Clients;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.data.Operations;
import com.github.carloszaldivar.distributedauth.models.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DistributedAuthApplicationTests {
    private Client client1;
    private Client client2;
    private Client client3;
    private Neighbour neighbour = new Neighbour("Server1", "localhost:10000");

    private Operation createFirstOperation(long timestamp) {
        Map<String, Object> firstClientData = new HashMap<>();
        firstClientData.put("number", client1.getNumber());
        firstClientData.put("pin", client1.getPin());
        firstClientData.put("activatedList", client1.getActivatedList().getPasswords());
        firstClientData.put("nonactivatedList", client1.getNonactivatedList().getPasswords());
        return new Operation(timestamp, Operation.Type.ADDING_CLIENT, 0, firstClientData, null);
    }

    private Operation createSecondOperation(long timestamp, Operation firstOperation, Client client) {
        Map<String, Object> secondClientData = new HashMap<>();
        secondClientData.put("number", client.getNumber());
        secondClientData.put("pin", client.getPin());
        secondClientData.put("activatedList", client.getActivatedList().getPasswords());
        secondClientData.put("nonactivatedList", client.getNonactivatedList().getPasswords());
        return new Operation(timestamp, Operation.Type.ADDING_CLIENT, 1, secondClientData, firstOperation);
    }

    public DistributedAuthApplicationTests() {
        String password = "password12";
        List<String> activePasswords = new ArrayList<>();
        List<String> inactivePasswords = new ArrayList<>();

        for (int i = 0; i < OneTimePasswordList.PASSWORDS_PER_LIST; ++i) {
            activePasswords.add(password);
            inactivePasswords.add(password);
        }
        client1 = new Client("123456", "1234", new OneTimePasswordList(activePasswords), new OneTimePasswordList(inactivePasswords));
        client2 = new Client("123457", "1235", new OneTimePasswordList(activePasswords), new OneTimePasswordList(inactivePasswords));
        client3 = new Client("123458", "1234", new OneTimePasswordList(activePasswords), new OneTimePasswordList(inactivePasswords));

        DistributedAuthApplication.setHistoryCleaning(false);
    }

    @Test
    public void fatRequestLocalEmptyTest() {
        NeighboursController neighboursController = new NeighboursController();
        neighboursController.addNeighbour(neighbour);

        Operation operation = createFirstOperation(100);
        FatRequest request = new FatRequest(neighbour.getId(), Collections.singletonList(operation), new HashMap<>());

        FatRequestController fatRequestController = new FatRequestController();
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.OK);
        Assert.assertEquals(operation, Operations.get().get(0));
        Assert.assertEquals(1, Clients.get().size());
    }

    @Test
    public void fatRequestEqualHistoryTest() {
        NeighboursController neighboursController = new NeighboursController();
        neighboursController.addNeighbour(neighbour);

        ClientsController clientsController = new ClientsController();
        clientsController.create(client1);

        FatRequest request = new FatRequest(neighbour.getId(), Collections.singletonList(Operations.get().get(0)), new HashMap<>());

        FatRequestController fatRequestController = new FatRequestController();
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.OK);
        Assert.assertEquals(1, Operations.get().size());
        Assert.assertEquals(1, Clients.get().size());
    }

    @Test
    public void fatRequestLocalTooOld() {
        NeighboursController neighboursController = new NeighboursController();
        neighboursController.addNeighbour(neighbour);

        ClientsController clientsController = new ClientsController();
        clientsController.create(client1);

        Operation firstOperation = Operations.get().get(0);
        List<Operation> history = Arrays.asList(firstOperation, createSecondOperation(firstOperation.getTimestamp() + 100, firstOperation, client2));
        FatRequest request = new FatRequest(neighbour.getId(), history, new HashMap<>());
        Assert.assertTrue(request.getHistory().get(0).isBefore(request.getHistory().get(1)));

        FatRequestController fatRequestController = new FatRequestController();
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.OK);
        Assert.assertEquals(2, Operations.get().size());
        Assert.assertEquals(2, Clients.get().size());
    }

    @Test
    public void fatRequestNeighbourTooOld() {
        NeighboursController neighboursController = new NeighboursController();
        neighboursController.addNeighbour(neighbour);

        ClientsController clientsController = new ClientsController();
        clientsController.create(client1);
        clientsController.create(client2);

        FatRequest request = new FatRequest(neighbour.getId(), Collections.singletonList(Operations.get().get(0)), new HashMap<>());

        FatRequestController fatRequestController = new FatRequestController();
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.U2OLD);
        Assert.assertEquals(2, Operations.get().size());
        Assert.assertEquals(2, Clients.get().size());
    }

    @Test
    public void fatRequestLocalNotCorrect() throws InterruptedException {
        NeighboursController neighboursController = new NeighboursController();
        neighboursController.addNeighbour(neighbour);

        ClientsController clientsController = new ClientsController();
        clientsController.create(client1);
        // It's safer to wait couple milliseconds before adding the second client locally, so that second client
        // on neighbour's machine has lower timestamp
        TimeUnit.MILLISECONDS.sleep(50L);
        clientsController.create(client3);

        Operation firstOperation = Operations.get().get(0);
        Operation secondOperation = createSecondOperation(firstOperation.getTimestamp() + 1, firstOperation, client2);
        FatRequest request = new FatRequest(neighbour.getId(), Arrays.asList(firstOperation, secondOperation), new HashMap<>());

        FatRequestController fatRequestController = new FatRequestController();
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.OK);
        Assert.assertEquals(2, Operations.get().size());
        Assert.assertEquals(secondOperation.getHash(), Operations.get().get(1).getHash());
    }

    @Test
    public void fatRequestNeighbourNotCorrect() {
        NeighboursController neighboursController = new NeighboursController();
        neighboursController.addNeighbour(neighbour);

        ClientsController clientsController = new ClientsController();
        clientsController.create(client1);
        clientsController.create(client2);

        Operation firstOperation = Operations.get().get(0);
        Operation thirdOperation = createSecondOperation(Operations.get().get(1).getTimestamp() + 100, firstOperation, client3);
        FatRequest request = new FatRequest(neighbour.getId(), Arrays.asList(firstOperation, thirdOperation), new HashMap<>());

        FatRequestController fatRequestController = new FatRequestController();
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.CONFLICT);
    }

    @Test
    public void thinRequestEqualHistoryTest() {
        NeighboursController neighboursController = new NeighboursController();
        neighboursController.addNeighbour(neighbour);

        String hash = "121fe00c89a1526306090e00542badce64b0f5d2d6e6c4c4eab5fae91b336577";
        ThinRequest request = new ThinRequest(neighbour.getId(), hash, new HashMap<>());

        Operation operation = createFirstOperation(100);
        Operations.get().add(operation);

        ThinRequestController thinRequestController = new ThinRequestController();
        ThinRequestResponse response = thinRequestController.handleThinRequest(request).getBody();
        Assert.assertEquals(ThinRequestResponse.Status.UPDATE_NOT_NEEDED, response.getStatus());
    }

    @Test
    public void thinRequestDifferentHistoryTest() {
        NeighboursController neighboursController = new NeighboursController();
        neighboursController.addNeighbour(neighbour);

        String hash = "a44b99bb6206ea2e45fe442819e30e37f521d99a98ed9a8319a4246214627b2d";
        ThinRequest request = new ThinRequest(neighbour.getId(), hash, new HashMap<>());

        ThinRequestController thinRequestController = new ThinRequestController();
        ThinRequestResponse response = thinRequestController.handleThinRequest(request).getBody();
        Assert.assertEquals(ThinRequestResponse.Status.UPDATE_NEEDED, response.getStatus());
    }

    @After
    public void cleanSingletons() {
        Clients.get().clear();
        Neighbours.get().clear();
        Neighbours.getSyncTimes().clear();
        Operations.get().clear();
    }
}
