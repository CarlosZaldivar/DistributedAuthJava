package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.controllers.NeighboursController;
import com.github.carloszaldivar.distributedauth.data.Clients;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.data.Operations;
import com.github.carloszaldivar.distributedauth.models.Neighbour;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class NeighboursControllerTests {
    @Test
    public void addingNeighbourTest() {
        Assert.assertEquals(0, Neighbours.get().size());
        NeighboursController controller = new NeighboursController();

        Neighbour neighbour = new Neighbour("Server1", "localhost:10000");
        controller.addNeighbour(neighbour);

        Assert.assertEquals(1, Neighbours.get().size());
        Assert.assertEquals(neighbour, Neighbours.get().get(0));
    }

    @After
    public void cleanSingletons() {
        Clients.get().clear();
        Neighbours.get().clear();
        Neighbours.getSyncTimes().clear();
        Operations.get().clear();
    }
}
