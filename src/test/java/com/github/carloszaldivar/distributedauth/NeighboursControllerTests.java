package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.controllers.NeighboursController;
import com.github.carloszaldivar.distributedauth.data.*;
import com.github.carloszaldivar.distributedauth.models.Neighbour;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class NeighboursControllerTests {
    private NeighboursRepository neighboursRepository = new LocalNeighboursRepository();
    private ClientsRepository clientsRepository = new LocalClientsRepository();
    private OperationsRepository operationsRepository = new LocalOperationsRepository();

    @Test
    public void addingNeighbourTest() {
        Assert.assertEquals(0, neighboursRepository.getNeighbours().size());
        NeighboursController controller = new NeighboursController(neighboursRepository);

        Neighbour neighbour = new Neighbour("Server1", "localhost:10000");
        controller.addNeighbour(neighbour);

        Assert.assertEquals(1, neighboursRepository.getNeighbours().size());
        Assert.assertEquals(neighbour, neighboursRepository.getNeighbours().get(neighbour.getId()));
    }

    @After
    public void cleanGlobalData() {
        neighboursRepository.clear();
        operationsRepository.clear();
        clientsRepository.clear();
        DistributedAuthApplication.setState(DistributedAuthApplication.State.SYNCHRONIZED);
    }
}
