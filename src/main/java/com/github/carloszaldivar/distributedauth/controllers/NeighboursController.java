package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.models.Neighbour;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class NeighboursController {
    @RequestMapping(method=POST, value={"/neighbours"})
    public Neighbour addNeighbour(@RequestBody Neighbour neighbour) {
        validateNeighbour(neighbour);
        Neighbours.get().add(neighbour);
        Neighbours.getSyncTimes().put(neighbour.getId(), 0L);
        return neighbour;
    }

    private void validateNeighbour(Neighbour neighbour) {
        if (Neighbours.getSyncTimes().containsKey(neighbour.getId())) {
            throw new InvalidParameterException("Neighbour already exists");
        }
    }

    @ExceptionHandler(InvalidParameterException.class)
    private Map<String, String> handleException(InvalidParameterException exception) {
        Map<String, String> error = new HashMap<>();
        error.put("error", exception.getMessage());
        return error;
    }
}
