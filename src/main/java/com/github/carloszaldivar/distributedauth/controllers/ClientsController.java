package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.communication.AuthRequestsSender;
import com.github.carloszaldivar.distributedauth.communication.FatRequestsSender;
import com.github.carloszaldivar.distributedauth.data.*;
import com.github.carloszaldivar.distributedauth.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class ClientsController {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.controllers.ClientsController");

    @Autowired
    private NeighboursRepository neighboursRepository;
    @Autowired
    private ClientsRepository clientsRepository;
    @Autowired
    private OperationsRepository operationsRepository;

    public ClientsController(NeighboursRepository neighboursRepository, ClientsRepository clientsRepository, OperationsRepository operationsRepository) {
        this.neighboursRepository = neighboursRepository;
        this.clientsRepository = clientsRepository;
        this.operationsRepository = operationsRepository;
    }

    @RequestMapping(method=POST, value={"/public/clients"})
    public ResponseEntity create(@RequestBody Client client) {
        checkServerState();
        validateClient(client);
        client.generateOneTimePasswordLists();
        operationsRepository.lockWrite();
        try {
            clientsRepository.add(client);
            addClientAddingOperation(System.currentTimeMillis(), client);
        } finally {
            operationsRepository.unlockWrite();
        }
        logger.info("Created client " + client.getNumber());
        (new FatRequestsSender(neighboursRepository, operationsRepository)).sendFatRequests();
        DistributedAuthApplication.setState(DistributedAuthApplication.State.UNSYNCHRONIZED);
        return new ResponseEntity(HttpStatus.CREATED);
    }

    @RequestMapping(method=GET, value={"/public/clients"})
    public ResponseEntity<Collection<Client>> list() {
        logger.info("Returning list of clients.");
        return new ResponseEntity<>(new ArrayList<>(clientsRepository.getAll().values()), HttpStatus.OK);
    }

    @RequestMapping(method=DELETE, value={"/public/clients/{id}"})
    public ResponseEntity delete(@PathVariable(value="id") String clientNumber, @RequestBody String pin) {
        checkServerState();
        validateClientNumber(clientNumber);
        if (!tryToAuthenticate(clientNumber, pin)) {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
        operationsRepository.lockWrite();
        try {
            Client clientToRemove = clientsRepository.get(clientNumber);
            clientsRepository.delete(clientNumber);
            addClientDeletingOperation(System.currentTimeMillis(), clientToRemove);
        } finally {
            operationsRepository.unlockWrite();
        }
        logger.info("Removed client " + clientNumber);
        (new FatRequestsSender(neighboursRepository, operationsRepository)).sendFatRequests();
        DistributedAuthApplication.updateState();
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(method=POST, value={"/public/clients/{id}/authenticate"})
    public ResponseEntity authenticate(@PathVariable(value="id") String clientNumber, @RequestBody String pin) {
        validateClientNumber(clientNumber);
        HttpStatus status = tryToAuthenticate(clientNumber, pin) ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        logger.info(String.format("Client %s authentication attempt. Result - %s", clientNumber, status));
        return new ResponseEntity(status);
    }

    @RequestMapping(method=POST, value={"/public/clients/{id}/authorize"})
    public ResponseEntity authorizeOperation(@PathVariable(value="id") String clientNumber, @RequestBody AuthorizationRequest request)
    {
        validateClientNumber(clientNumber);
        validateAuthorizationData(request);
        Client client = clientsRepository.get(clientNumber);
        boolean isAuthorized = tryToAuthorizeOperation(client, request);
        HttpStatus status = isAuthorized ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        logger.info(String.format("Client's %s operation authorization attempt. Result - %s", clientNumber, status));
        if (isAuthorized) {
            (new FatRequestsSender(neighboursRepository, operationsRepository)).sendFatRequests();
        }
        DistributedAuthApplication.updateState();
        return new ResponseEntity(status);
    }

    @RequestMapping(method=POST, value={"/public/clients/{id}/activatelist"})
    public ResponseEntity activateNewPasswordList(@PathVariable(value="id") String clientNumber, @RequestBody AuthorizationRequest request) {
        validateClientNumber(clientNumber);
        validateAuthorizationData(request);
        Client client = clientsRepository.get(clientNumber);
        boolean isActivated = tryToActivateNewPasswordList(client, request);
        HttpStatus status = isActivated ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        logger.info(String.format("Client's %s new password list activation attempt. Result - %s", clientNumber, status));
        if (isActivated) {
            (new FatRequestsSender(neighboursRepository, operationsRepository)).sendFatRequests();
        }
        DistributedAuthApplication.updateState();
        return new ResponseEntity(status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    private ResponseEntity<String> handleException(IllegalArgumentException exception) {
        return handleException((Exception) exception);
    }

    @ExceptionHandler(IllegalStateException.class)
    private ResponseEntity<String> handleException(IllegalStateException exception) {
        return handleException((Exception) exception);
    }

    @ExceptionHandler(RuntimeException.class)
    private ResponseEntity<String> handleException(RuntimeException exception) {
        return handleException((Exception) exception);
    }

    private ResponseEntity<String> handleException(Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private void checkServerState() {
        if (DistributedAuthApplication.getState() == DistributedAuthApplication.State.CONFLICT ||
                DistributedAuthApplication.getState() == DistributedAuthApplication.State.TOO_OLD) {
            throw new IllegalStateException("Server is in invalid state. Waiting for updates.");
        }
    }

    private void validateClient(Client client)
    {
        String number = client.getNumber();
        if (!(number.matches("[0-9]+") && number.length() == 6)) {
            throw new IllegalArgumentException("Client number should consist of 6 digits.");
        }

        String pin = client.getPin();
        if (!(pin.matches("[0-9]+") && pin.length() == 4))
        {
            throw new IllegalArgumentException("PIN should consist of 4 digits.");
        }

        if (clientsRepository.get(number) != null) {
            throw new IllegalArgumentException("Client with this number already exists.");
        }
    }


    private void validateClientNumber(String clientNumber) {
        if (!clientsRepository.getAll().containsKey(clientNumber)) {
            throw new IllegalArgumentException("No client with number " + clientNumber);
        }
    }

    private void validateAuthorizationData(AuthorizationRequest request) {
        if (request.getPin() == null || request.getOneTimePassword() == null) {
            throw new IllegalArgumentException("PIN and password should be provided.");
        }
    }

    private boolean tryToAuthenticate(String clientNumber, String pin) {
        List<Neighbour> specialNeighbours = neighboursRepository.getNeighbours().values().stream().filter(Neighbour::isSpecial).collect(Collectors.toList());
        AuthRequestsSender requestsSender = new AuthRequestsSender();
        for (Neighbour specialNeighbour : specialNeighbours) {
            if (!requestsSender.neighbourAgreesToAuthenticate(specialNeighbour, clientNumber, pin)) {
                return false;
            }
        }
        return clientsRepository.authenticateClient(clientNumber, pin);
    }

    private boolean tryToAuthorizeOperation(Client client, AuthorizationRequest request) {
        List<Neighbour> specialNeighbours = neighboursRepository.getNeighbours().values().stream().filter(Neighbour::isSpecial).collect(Collectors.toList());
        AuthRequestsSender requestsSender = new AuthRequestsSender();
        for (Neighbour specialNeighbour : specialNeighbours) {
            if (!requestsSender.neighbourAgreesToAuthorizeOperation(specialNeighbour, client.getNumber(), request)) {
                return false;
            }
        }

        Client clientBefore = new Client(client);
        operationsRepository.lockWrite();
        try {
            boolean isAuthorized = clientsRepository.authorizeOperation(client.getNumber(), request.getPin(), request.getOneTimePassword());
            if (isAuthorized) {
                addAuthorizingOperation(System.currentTimeMillis(), clientBefore, client);
            }
            return isAuthorized;
        } finally {
            operationsRepository.unlockWrite();
        }
    }

    private boolean tryToActivateNewPasswordList(Client client, AuthorizationRequest request) {
        List<Neighbour> specialNeighbours = neighboursRepository.getNeighbours().values().stream().filter(Neighbour::isSpecial).collect(Collectors.toList());
        AuthRequestsSender requestsSender = new AuthRequestsSender();
        for (Neighbour specialNeighbour : specialNeighbours) {
            if (!requestsSender.neighbourAgreesToActivateNewPasswordList(specialNeighbour, client.getNumber(), request)) {
                return false;
            }
        }

        Client clientBefore = new Client(client);
        operationsRepository.lockWrite();
        try {
            boolean isAuthorized = clientsRepository.activateNewPasswordList(client.getNumber(), request.getPin(), request.getOneTimePassword());
            if (isAuthorized) {
                addListActivationOperation(System.currentTimeMillis(), clientBefore, client);
            }
            return isAuthorized;
        } finally {
            operationsRepository.unlockWrite();
        }
    }

    private void addClientAddingOperation(long unixTimestamp, Client client) {
        Operation lastOperation = operationsRepository.getLast();
        int number = lastOperation != null ? lastOperation.getNumber() + 1 : 0;
        Operation newOperation = new Operation(unixTimestamp, Operation.Type.ADD_CLIENT, number, null, new Client(client), lastOperation);
        operationsRepository.addToEnd(newOperation);
    }

    private void addClientDeletingOperation(long unixTimestamp, Client client) {
        Operation lastOperation = operationsRepository.getLast();
        int number = lastOperation != null ? lastOperation.getNumber() + 1 : 0;
        Operation newOperation =  new Operation(unixTimestamp, Operation.Type.DELETE_CLIENT, number, new Client(client), null, lastOperation);
        operationsRepository.addToEnd(newOperation);
    }


    private void addAuthorizingOperation(long unixTimestamp, Client clientBefore, Client clientAfter) {
        Operation lastOperation = operationsRepository.getLast();
        int number = lastOperation != null ? lastOperation.getNumber() + 1 : 0;
        Operation newOperation = new Operation(unixTimestamp, Operation.Type.AUTHORIZATION, number,
                new Client(clientBefore), new Client(clientAfter), lastOperation);
        operationsRepository.addToEnd(newOperation);
    }

    private void addListActivationOperation(long unixTimestamp, Client clientBefore, Client clientAfter) {
        Operation lastOperation = operationsRepository.getLast();
        int number = lastOperation != null ? lastOperation.getNumber() + 1 : 0;
        Operation newOperation = new Operation(unixTimestamp, Operation.Type.LIST_ACTIVATION, number,
                new Client(clientBefore), new Client(clientAfter), lastOperation);
        operationsRepository.addToEnd(newOperation);
    }
}