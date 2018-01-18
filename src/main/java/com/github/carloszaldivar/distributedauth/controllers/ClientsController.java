package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.communication.AuthRequestsSender;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.communication.FatRequestsSender;
import com.github.carloszaldivar.distributedauth.models.*;
import com.github.carloszaldivar.distributedauth.data.Clients;
import com.github.carloszaldivar.distributedauth.data.Operations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @RequestMapping(method=POST, value={"/clients"})
    public ResponseEntity create(@RequestBody Client client) {
        checkServerState();
        validateClient(client);
        client.generateOneTimePasswordLists();
        Clients.get().put(client.getNumber(), client);
        Operation addingClientOperation = createClientAddingOperation(System.currentTimeMillis(), client);
        Operations.get().add(addingClientOperation);
        logger.info("Created client " + client.getNumber());
        (new FatRequestsSender()).sendFatRequests();
        DistributedAuthApplication.setState(DistributedAuthApplication.State.UNSYNCHRONIZED);
        return new ResponseEntity(HttpStatus.CREATED);
    }

    @RequestMapping(method=GET, value={"/clients"})
    public ResponseEntity<List<Client>> list() {
        logger.info("Returning list of clients.");
        return new ResponseEntity<>(new ArrayList<>(Clients.get().values()), HttpStatus.OK);
    }

    @RequestMapping(method=DELETE, value={"/clients/{id}"})
    public ResponseEntity delete(@PathVariable(value="id") String clientNumber) {
        checkServerState();
        validateClientNumber(clientNumber);
        Client client = Clients.get().get(clientNumber);
        Operation deletingClientOperation = createClientDeletingOperation(System.currentTimeMillis(), client);
        Clients.get().remove(clientNumber);
        Operations.get().add(deletingClientOperation);
        logger.info("Removed client " + client.getNumber());
        (new FatRequestsSender()).sendFatRequests();
        DistributedAuthApplication.setState(DistributedAuthApplication.State.UNSYNCHRONIZED);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(method=POST, value={"/clients/{id}/authenticate"})
    public ResponseEntity authenticate(@PathVariable(value="id") String clientNumber, @RequestBody String pin) {
        validateClientNumber(clientNumber);
        HttpStatus status = tryToAuthenticate(clientNumber, pin) ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        logger.info(String.format("Client %s authentication attempt. Result - %s", clientNumber, status));
        return new ResponseEntity(status);
    }

    @RequestMapping(method=POST, value={"/clients/{id}/authorize"})
    public ResponseEntity authorizeOperation(@PathVariable(value="id") String clientNumber, @RequestBody AuthorizationRequest request)
    {
        validateClientNumber(clientNumber);
        validateAuthorizationData(request);
        Client client = Clients.get().get(clientNumber);
        Client clientBefore = new Client(client);
        boolean isAuthorized = tryToAuthorizeOperation(client, request);
        if (isAuthorized) {
            Operation authorizingOperation = createAuthorizingOperation(System.currentTimeMillis(), clientBefore, client);
            Operations.get().add(authorizingOperation);
        }
        HttpStatus status = isAuthorized ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        logger.info(String.format("Client's %s operation authorization attempt. Result - %s", clientNumber, status));
        return new ResponseEntity(status);
    }

    @RequestMapping(method=POST, value={"/clients/{id}/activatelist"})
    public ResponseEntity activateNewPasswordList(@PathVariable(value="id") String clientNumber, @RequestBody AuthorizationRequest request) {
        validateClientNumber(clientNumber);
        validateAuthorizationData(request);
        Client client = Clients.get().get(clientNumber);
        Client clientBefore = new Client(client);

        boolean isActivated = tryToActivateNewPasswordList(client, request);
        if (isActivated) {
            Operation listActivationOperation = createListActivationOperation(System.currentTimeMillis(), clientBefore, client);
            Operations.get().add(listActivationOperation);
        }
        HttpStatus status = isActivated ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        logger.info(String.format("Client's %s new password list activation attempt. Result - %s", clientNumber, status));
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

        if (Clients.get().containsKey(number)) {
            throw new IllegalArgumentException("Client with this number already exists.");
        }
    }


    private void validateClientNumber(String clientNumber) {
        if (!Clients.get().containsKey(clientNumber)) {
            throw new IllegalArgumentException("No client with number " + clientNumber);
        }
    }

    private void validateAuthorizationData(AuthorizationRequest request) {
        if (request.getPin() == null || request.getOneTimePassword() == null) {
            throw new IllegalArgumentException("PIN and password should be provided.");
        }
    }

    private Operation createClientAddingOperation(long unixTimestamp, Client client) {
        List<Operation> operations = Operations.get();
        Operation lastOperation = null;
        int number = 0;

        if (!operations.isEmpty()) {
            lastOperation = Operations.get().get(Operations.get().size() - 1);
            number = lastOperation.getNumber();
        }

        return new Operation(unixTimestamp, Operation.Type.ADDING_CLIENT, number, null, client, lastOperation);
    }

    private Operation createClientDeletingOperation(long unixTimestamp, Client client) {
        List<Operation> operations = Operations.get();
        Operation lastOperation = null;
        int number = 0;

        if (!operations.isEmpty()) {
            lastOperation = Operations.get().get(Operations.get().size() - 1);
            number = lastOperation.getNumber();
        }

        return new Operation(unixTimestamp, Operation.Type.REMOVING_CLIENT, number, client, null, lastOperation);
    }

    private Operation createAuthorizingOperation(long unixTimestamp, Client clientBefore, Client clientAfter) {
        List<Operation> operations = Operations.get();
        Operation lastOperation = null;
        int number = 0;

        if (!operations.isEmpty()) {
            lastOperation = Operations.get().get(Operations.get().size() - 1);
            number = lastOperation.getNumber();
        }

        return new Operation(unixTimestamp, Operation.Type.AUTHORIZATION, number, clientBefore, clientAfter, lastOperation);
    }

    private Operation createListActivationOperation(long unixTimestamp, Client clientBefore, Client clientAfter) {
        List<Operation> operations = Operations.get();
        Operation lastOperation = null;
        int number = 0;

        if (!operations.isEmpty()) {
            lastOperation = Operations.get().get(Operations.get().size() - 1);
            number = lastOperation.getNumber();
        }

        return new Operation(unixTimestamp, Operation.Type.LIST_ACTIVATION, number, clientBefore, clientAfter, lastOperation);
    }

    private boolean tryToAuthenticate(String clientNumber, String pin) {
        List<Neighbour> specialNeighbours = Neighbours.get().values().stream().filter(Neighbour::isSpecial).collect(Collectors.toList());
        AuthRequestsSender requestsSender = new AuthRequestsSender();
        for (Neighbour specialNeighbour : specialNeighbours) {
            if (!requestsSender.neighbourAgreesToAuthenticate(specialNeighbour, clientNumber, pin)) {
                return false;
            }
        }
        return Clients.get().get(clientNumber).getPin().equals(pin);
    }

    private boolean tryToAuthorizeOperation(Client client, AuthorizationRequest request) {
        List<Neighbour> specialNeighbours = Neighbours.get().values().stream().filter(Neighbour::isSpecial).collect(Collectors.toList());
        AuthRequestsSender requestsSender = new AuthRequestsSender();
        for (Neighbour specialNeighbour : specialNeighbours) {
            if (!requestsSender.neighbourAgreesToAuthorizeOperation(specialNeighbour, client.getNumber(), request)) {
                return false;
            }
        }
        return request.getPin().equals(client.getPin()) && client.useOneTimePassword(request.getOneTimePassword());
    }

    private boolean tryToActivateNewPasswordList(Client client, AuthorizationRequest request) {
        List<Neighbour> specialNeighbours = Neighbours.get().values().stream().filter(Neighbour::isSpecial).collect(Collectors.toList());
        AuthRequestsSender requestsSender = new AuthRequestsSender();
        for (Neighbour specialNeighbour : specialNeighbours) {
            if (!requestsSender.neighbourAgreesToActivateNewPasswordList(specialNeighbour, client.getNumber(), request)) {
                return false;
            }
        }
        return request.getPin().equals(client.getPin()) && client.activateNewOneTimePasswordList(request.getOneTimePassword());
    }
}