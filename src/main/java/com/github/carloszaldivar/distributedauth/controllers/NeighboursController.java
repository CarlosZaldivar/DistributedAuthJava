package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.data.NeighboursRepository;
import com.github.carloszaldivar.distributedauth.models.Neighbour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class NeighboursController {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.controllers.NeighboursController");
    @Autowired
    private NeighboursRepository neighboursRepository;

    public NeighboursController(NeighboursRepository neighboursRepository) {
        this.neighboursRepository = neighboursRepository;
    }

    @RequestMapping(method=POST, value={"/protected/neighbours"})
    public ResponseEntity addNeighbour(@RequestBody Neighbour neighbour) {
        validateNeighbour(neighbour);
        neighboursRepository.add(neighbour);
        logger.info(String.format("Neighbour %s with URL %s added.", neighbour.getId(), neighbour.getUrl()));
        DistributedAuthApplication.updateState();
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(method=GET, value={"/protected/neighbours"})
    public ResponseEntity<Collection<Neighbour>> listNeighbours() {
        logger.info("Returning list of neighbours");
        return new ResponseEntity<>(neighboursRepository.getNeighbours().values(), HttpStatus.OK);
    }

    @RequestMapping(method=POST, value={"/protected/neighbours/{id}/special"})
    public ResponseEntity setSpecial(@PathVariable(value="id") String neighbourId, @RequestBody boolean special) {
        validateNeighbourId(neighbourId);
        neighboursRepository.setSpecial(neighbourId, true);
        return new ResponseEntity(HttpStatus.OK);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    private ResponseEntity<String> handleException(IllegalArgumentException exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private void validateNeighbour(Neighbour neighbour) {
        if (neighboursRepository.getSyncTimes().containsKey(neighbour.getId())) {
            throw new IllegalArgumentException("Neighbour already exists");
        }
    }

    private void validateNeighbourId(String neighbourId) {
        if (!neighboursRepository.getSyncTimes().containsKey(neighbourId)) {
            throw new IllegalArgumentException("No neighbour with id " + neighbourId);
        }
    }
}
