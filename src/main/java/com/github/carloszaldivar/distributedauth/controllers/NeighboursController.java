package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.models.Neighbour;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NeighboursController {
    public Neighbour addNeighbour(Neighbour neighbour) {
        Neighbours.get().add(neighbour);
        Neighbours.getSyncTimes().put(neighbour.getId(), 0L);
        return neighbour;
    }
}
