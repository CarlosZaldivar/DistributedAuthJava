package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.models.Neighbour;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class NeighboursController {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.controllers.NeighboursController");

    @RequestMapping(method=POST, value={"/neighbours"})
    public ResponseEntity addNeighbour(@RequestBody Neighbour neighbour) {
        validateNeighbour(neighbour);
        Neighbours.get().put(neighbour.getId(), neighbour);
        Neighbours.getSyncTimes().put(neighbour.getId(), 0L);
        logger.info(String.format("Neighbour %s with URL %s added.", neighbour.getId(), neighbour.getUrl()));
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(method=GET, value={"/neighbours"})
    public ResponseEntity<Collection<Neighbour>> listNeighbours() {
        logger.info("Returning list of neighbours");
        return new ResponseEntity<>(Neighbours.get().values(), HttpStatus.OK);
    }

    @RequestMapping(method=POST, value={"neighbours/{id}/special"})
    public ResponseEntity setSpecial(@PathVariable(value="id") String neighbourId, @RequestBody boolean special) {
        validateNeighbourId(neighbourId);
        Neighbours.get().get(neighbourId).setSpecial(special);
        return new ResponseEntity(HttpStatus.OK);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    private ResponseEntity<String> handleException(IllegalArgumentException exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private void validateNeighbour(Neighbour neighbour) {
        if (Neighbours.getSyncTimes().containsKey(neighbour.getId())) {
            throw new IllegalArgumentException("Neighbour already exists");
        }
    }

    private void validateNeighbourId(String neighbourId) {
        if (!Neighbours.get().containsKey(neighbourId)) {
            throw new IllegalArgumentException("No neighbour with id " + neighbourId);
        }
    }
}
