package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.controllers.NeighboursController;
import com.github.carloszaldivar.distributedauth.data.*;
import com.github.carloszaldivar.distributedauth.models.Neighbour;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class NeighboursControllerTests {
    private NeighboursRepository neighboursRepository = new LocalNeighboursRepository();

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
    public void cleanSingletons() {
        Clients.get().clear();
        neighboursRepository.clear();
        Operations.get().clear();
    }
}
