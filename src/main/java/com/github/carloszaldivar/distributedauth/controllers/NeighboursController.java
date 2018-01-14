package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.models.Neighbour;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class NeighboursController {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.controllers.NeighboursController");

    @RequestMapping(method=POST, value={"/neighbours"})
    public Neighbour addNeighbour(@RequestBody Neighbour neighbour) {
        validateNeighbour(neighbour);
        Neighbours.get().add(neighbour);
        Neighbours.getSyncTimes().put(neighbour.getId(), 0L);
        logger.info(String.format("Neighbour %s with URL %s added.", neighbour.getId(), neighbour.getUrl()));
        return neighbour;
    }

    @RequestMapping(method=GET, value={"/neighbours"})
    public List<Neighbour> listNeighbours() {
        logger.info("Returning list of neighbours");
        return Neighbours.get();
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
