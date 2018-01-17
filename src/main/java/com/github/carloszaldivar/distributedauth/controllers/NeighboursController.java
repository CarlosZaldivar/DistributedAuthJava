package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.models.Neighbour;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class NeighboursController {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.controllers.NeighboursController");

    @RequestMapping(method=POST, value={"/neighbours"})
    public ResponseEntity<Neighbour> addNeighbour(@RequestBody Neighbour neighbour) {
        validateNeighbour(neighbour);
        Neighbours.get().add(neighbour);
        Neighbours.getSyncTimes().put(neighbour.getId(), 0L);
        logger.info(String.format("Neighbour %s with URL %s added.", neighbour.getId(), neighbour.getUrl()));
        return new ResponseEntity<>(neighbour, HttpStatus.OK);
    }

    @RequestMapping(method=GET, value={"/neighbours"})
    public ResponseEntity<List<Neighbour>> listNeighbours() {
        logger.info("Returning list of neighbours");
        return new ResponseEntity<>(Neighbours.get(), HttpStatus.OK);
    }

    private void validateNeighbour(Neighbour neighbour) {
        if (Neighbours.getSyncTimes().containsKey(neighbour.getId())) {
            throw new IllegalArgumentException("Neighbour already exists");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    private ResponseEntity<String> handleException(IllegalArgumentException exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
