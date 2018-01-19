package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.communication.ThinRequestsSender;
import com.github.carloszaldivar.distributedauth.data.ClientsRepository;
import com.github.carloszaldivar.distributedauth.data.NeighboursRepository;
import com.github.carloszaldivar.distributedauth.data.OperationsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class DebugController {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.controllers.DebugController");

    @Autowired
    private NeighboursRepository neighboursRepository;
    @Autowired
    private ClientsRepository clientsRepository;
    @Autowired
    private OperationsRepository operationsRepository;

    @RequestMapping(method=POST, value={"/protected/debug/clear"})
    public ResponseEntity clear() {
        logger.info("Clearing server data.");
        neighboursRepository.clear();
        clientsRepository.clear();
        operationsRepository.clear();
        DistributedAuthApplication.setState(DistributedAuthApplication.State.SYNCHRONIZED);
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(method=POST, value={"/protected/debug/thinrequests"})
    public ResponseEntity thinRequestsStatus(@RequestBody boolean status) {
        if (status) {
            logger.info("Enabling ThinRequestsSender.");
        } else {
            logger.info("Disabling ThinRequestsSender.");
        }
        ThinRequestsSender.setEnabled(status);
        return new ResponseEntity(HttpStatus.OK);
    }
}
