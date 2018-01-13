package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.controllers.ClientsController;
import com.github.carloszaldivar.distributedauth.controllers.NeighboursController;
import com.github.carloszaldivar.distributedauth.controllers.SynchronizationController;
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
    private Client client1 = new Client("123456", "1234");
    private Client client2 = new Client("123457", "1235");
    private Client client3 = new Client("123458", "1234");
    private Neighbour neighbour = new Neighbour("Server1", "localhost:10000");

    private Operation createFirstOperation(long timestamp) {
        Map<String, Object> firstClientData = new HashMap<>();
        firstClientData.put("number", client1.getNumber());
        firstClientData.put("pin", client1.getPin());
        return new Operation(timestamp, Operation.Type.ADDING_CLIENT, 0, firstClientData, null);
    }

    private Operation createSecondOperation(long timestamp, Operation firstOperation) {
        Map<String, Object> secondClientData = new HashMap<>();
        secondClientData.put("number", client2.getNumber());
        secondClientData.put("pin", client2.getPin());
        return new Operation(timestamp, Operation.Type.ADDING_CLIENT, 1, secondClientData, firstOperation);
    }

    private Operation createThirdOperation(long timestamp, Operation firstOperation) {
        Map<String, Object> thirdClientData = new HashMap<>();
        thirdClientData.put("number", client3.getNumber());
        thirdClientData.put("pin", client3.getPin());
        // Adding third client conflicts with adding second client.
        return new Operation(timestamp, Operation.Type.ADDING_CLIENT, 1, thirdClientData, firstOperation);
    }

    @Test
    public void fatRequestLocalEmptyTest() {
        NeighboursController neighboursController = new NeighboursController();
        neighboursController.addNeighbour(neighbour);

        FatRequest request = new FatRequest();
        request.setSenderId(neighbour.getId());

        Operation operation = createFirstOperation(100);
        request.setHistory(Collections.singletonList(operation));
        request.setSyncTimes(new HashMap<>());

        SynchronizationController synchronizationController = new SynchronizationController();
        FatRequestResponse response = synchronizationController.handleFatRequest(request);

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

        FatRequest request = new FatRequest();
        request.setSenderId(neighbour.getId());
        request.setHistory(Collections.singletonList(Operations.get().get(0)));
        request.setSyncTimes(new HashMap<>());

        SynchronizationController synchronizationController = new SynchronizationController();
        FatRequestResponse response = synchronizationController.handleFatRequest(request);

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

        FatRequest request = new FatRequest();
        request.setSenderId(neighbour.getId());
        Operation firstOperation = Operations.get().get(0);
        request.setHistory(Arrays.asList(firstOperation, createSecondOperation(firstOperation.getTimestamp() + 100, firstOperation)));
        Assert.assertTrue(request.getHistory().get(0).isBefore(request.getHistory().get(1)));
        request.setSyncTimes(new HashMap<>());

        SynchronizationController synchronizationController = new SynchronizationController();
        FatRequestResponse response = synchronizationController.handleFatRequest(request);

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

        FatRequest request = new FatRequest();
        request.setSenderId(neighbour.getId());
        request.setHistory(Collections.singletonList(Operations.get().get(0)));
        request.setSyncTimes(new HashMap<>());

        SynchronizationController synchronizationController = new SynchronizationController();
        FatRequestResponse response = synchronizationController.handleFatRequest(request);

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

        FatRequest request = new FatRequest();
        request.setSenderId(neighbour.getId());
        Operation firstOperation = Operations.get().get(0);

        Operation secondOperation = createSecondOperation(firstOperation.getTimestamp() + 1, firstOperation);
        request.setHistory(Arrays.asList(firstOperation, secondOperation));
        request.setSyncTimes(new HashMap<>());

        SynchronizationController synchronizationController = new SynchronizationController();
        FatRequestResponse response = synchronizationController.handleFatRequest(request);

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

        FatRequest request = new FatRequest();
        request.setSenderId(neighbour.getId());
        Operation firstOperation = Operations.get().get(0);
        Operation thirdOperation = createThirdOperation(Operations.get().get(1).getTimestamp() + 100, firstOperation);
        request.setHistory(Arrays.asList(firstOperation, thirdOperation));
        request.setSyncTimes(new HashMap<>());

        SynchronizationController synchronizationController = new SynchronizationController();
        FatRequestResponse response = synchronizationController.handleFatRequest(request);

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.CONFLICT);
    }

    @After
    public void cleanSingletons() {
        Clients.get().clear();
        Neighbours.get().clear();
        Neighbours.getSyncTimes().clear();
        Operations.get().clear();
    }
}
