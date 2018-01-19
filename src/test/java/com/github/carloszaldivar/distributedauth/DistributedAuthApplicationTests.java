package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.controllers.ClientsController;
import com.github.carloszaldivar.distributedauth.controllers.NeighboursController;
import com.github.carloszaldivar.distributedauth.controllers.FatRequestController;
import com.github.carloszaldivar.distributedauth.controllers.ThinRequestController;
import com.github.carloszaldivar.distributedauth.data.*;
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
    private NeighboursRepository neighboursRepository = new LocalNeighboursRepository();
    private ClientsRepository clientsRepository = new LocalClientsRepository();
    private OperationsRepository operationsRepository = new LocalOperationsRepository();

    private Operation createFirstOperation(long timestamp) {
        return new Operation(timestamp, Operation.Type.ADD_CLIENT, 0, null, client1, null);
    }

    private Operation createSecondOperation(long timestamp, Operation firstOperation, Client client) {
        return new Operation(timestamp, Operation.Type.ADD_CLIENT, 1, null, client2, firstOperation);
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
        NeighboursController neighboursController = new NeighboursController(neighboursRepository);
        neighboursController.addNeighbour(neighbour);

        Operation operation = createFirstOperation(100);
        FatRequest request = new FatRequest(neighbour.getId(), Collections.singletonList(operation), new HashMap<>(), System.currentTimeMillis());

        FatRequestController fatRequestController = new FatRequestController(neighboursRepository, clientsRepository, operationsRepository);
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.OK);
        Assert.assertEquals(operation, operationsRepository.getLast());
        Assert.assertEquals(1, clientsRepository.getAll().size());
    }

    @Test
    public void fatRequestEqualHistoryTest() {
        NeighboursController neighboursController = new NeighboursController(neighboursRepository);
        neighboursController.addNeighbour(neighbour);

        ClientsController clientsController = new ClientsController(neighboursRepository, clientsRepository, operationsRepository);
        clientsController.create(client1);

        FatRequest request = new FatRequest(neighbour.getId(), Collections.singletonList(operationsRepository.getAll().get(0)), new HashMap<>(), System.currentTimeMillis());

        FatRequestController fatRequestController = new FatRequestController(neighboursRepository, clientsRepository, operationsRepository);
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.ALREADY_SYNC);
        Assert.assertEquals(1, operationsRepository.getAll().size());
        Assert.assertEquals(1, clientsRepository.getAll().size());
    }

    @Test
    public void fatRequestLocalTooOld() {
        NeighboursController neighboursController = new NeighboursController(neighboursRepository);
        neighboursController.addNeighbour(neighbour);

        ClientsController clientsController = new ClientsController(neighboursRepository, clientsRepository, operationsRepository);
        clientsController.create(client1);

        Operation firstOperation = operationsRepository.getLast();
        List<Operation> history = Arrays.asList(firstOperation, createSecondOperation(firstOperation.getTimestamp() + 100, firstOperation, client2));
        FatRequest request = new FatRequest(neighbour.getId(), history, new HashMap<>(), System.currentTimeMillis());
        Assert.assertTrue(request.getHistory().get(0).isBefore(request.getHistory().get(1)));

        FatRequestController fatRequestController = new FatRequestController(neighboursRepository, clientsRepository, operationsRepository);
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.OK);
        Assert.assertEquals(2, operationsRepository.getAll().size());
        Assert.assertEquals(2, operationsRepository.getAll().size());
    }

    @Test
    public void fatRequestNeighbourTooOld() {
        NeighboursController neighboursController = new NeighboursController(neighboursRepository);
        neighboursController.addNeighbour(neighbour);

        ClientsController clientsController = new ClientsController(neighboursRepository, clientsRepository, operationsRepository);
        clientsController.create(client1);
        clientsController.create(client2);

        FatRequest request = new FatRequest(neighbour.getId(), Collections.singletonList(operationsRepository.getAll().get(0)), new HashMap<>(), System.currentTimeMillis());

        FatRequestController fatRequestController = new FatRequestController(neighboursRepository, clientsRepository, operationsRepository);
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.U2OLD);
        Assert.assertEquals(2, operationsRepository.getAll().size());
        Assert.assertEquals(2, clientsRepository.getAll().size());
    }

    @Test
    public void fatRequestLocalNotCorrect() throws InterruptedException {
        NeighboursController neighboursController = new NeighboursController(neighboursRepository);
        neighboursController.addNeighbour(neighbour);

        ClientsController clientsController = new ClientsController(neighboursRepository, clientsRepository, operationsRepository);
        clientsController.create(client1);
        // It's safer to wait couple milliseconds before adding the second client locally, so that second client
        // on neighbour's machine has lower timestamp
        TimeUnit.MILLISECONDS.sleep(50L);
        clientsController.create(client3);

        Operation firstOperation = operationsRepository.getAll().get(0);
        Operation secondOperation = createSecondOperation(firstOperation.getTimestamp() + 1, firstOperation, client2);
        FatRequest request = new FatRequest(neighbour.getId(), Arrays.asList(firstOperation, secondOperation), new HashMap<>(), System.currentTimeMillis());

        FatRequestController fatRequestController = new FatRequestController(neighboursRepository, clientsRepository, operationsRepository);
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.FIXED);
        Assert.assertEquals(2, operationsRepository.getAll().size());
        Assert.assertEquals(secondOperation.getHash(), operationsRepository.getAll().get(1).getHash());
    }

    @Test
    public void fatRequestNeighbourNotCorrect() {
        NeighboursController neighboursController = new NeighboursController(neighboursRepository);
        neighboursController.addNeighbour(neighbour);

        ClientsController clientsController = new ClientsController(neighboursRepository, clientsRepository, operationsRepository);
        clientsController.create(client1);
        clientsController.create(client2);

        List<Operation> operations = operationsRepository.getAll();
        Operation firstOperation = operations.get(0);
        Operation thirdOperation = createSecondOperation(operations.get(1).getTimestamp() + 100, firstOperation, client3);
        FatRequest request = new FatRequest(neighbour.getId(), Arrays.asList(firstOperation, thirdOperation), new HashMap<>(), System.currentTimeMillis());

        FatRequestController fatRequestController = new FatRequestController(neighboursRepository, clientsRepository, operationsRepository);
        FatRequestResponse response = fatRequestController.handleFatRequest(request).getBody();

        Assert.assertTrue(response.getStatus() == FatRequestResponse.Status.CONFLICT);
    }

    @Test
    public void thinRequestEqualHistoryTest() {
        NeighboursController neighboursController = new NeighboursController(neighboursRepository);
        neighboursController.addNeighbour(neighbour);

        String hash = "b4849c554df7985fd99c8071d5386622377e7f2d178ec321dc28d0c7ae7a83d3";
        ThinRequest request = new ThinRequest(neighbour.getId(), hash, new HashMap<>(), System.currentTimeMillis());

        Operation operation = createFirstOperation(100);
        operationsRepository.addToEnd(operation);

        ThinRequestController thinRequestController = new ThinRequestController(neighboursRepository, operationsRepository);
        ThinRequestResponse response = thinRequestController.handleThinRequest(request).getBody();
        Assert.assertEquals(ThinRequestResponse.Status.ALREADY_SYNC, response.getStatus());
    }

    @Test
    public void thinRequestDifferentHistoryTest() {
        NeighboursController neighboursController = new NeighboursController(neighboursRepository);
        neighboursController.addNeighbour(neighbour);

        String hash = "a44b99bb6206ea2e45fe442819e30e37f521d99a98ed9a8319a4246214627b2d";
        ThinRequest request = new ThinRequest(neighbour.getId(), hash, new HashMap<>(), System.currentTimeMillis());

        ThinRequestController thinRequestController = new ThinRequestController(neighboursRepository, operationsRepository);
        ThinRequestResponse response = thinRequestController.handleThinRequest(request).getBody();
        Assert.assertEquals(ThinRequestResponse.Status.NEED_SYNC, response.getStatus());
    }

    @After
    public void cleanGlobalData() {
        clientsRepository.clear();
        neighboursRepository.clear();
        operationsRepository.clear();
        DistributedAuthApplication.setState(DistributedAuthApplication.State.SYNCHRONIZED);
    }
}
