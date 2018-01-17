package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.synchronization.FatRequestsSender;
import com.github.carloszaldivar.distributedauth.models.*;
import com.github.carloszaldivar.distributedauth.data.Clients;
import com.github.carloszaldivar.distributedauth.data.Operations;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        addPasswordLists(client);
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
        if (clientNumber == null || pin == null) {
            throw new InvalidParameterException("Client number and PIN should be provided.");
        }

        HttpStatus httpStatus;
        if (!Clients.get().containsKey(clientNumber)) {
            httpStatus = HttpStatus.NOT_FOUND;
        } else {
            httpStatus = Clients.get().get(clientNumber).getPin().equals(pin) ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        }
        return new ResponseEntity(httpStatus);
    }

    @ExceptionHandler({InvalidParameterException.class, IllegalStateException.class})
    private ResponseEntity<String> handleException(InvalidParameterException exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
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
            throw new InvalidParameterException("Client number should consist of 6 digits.");
        }

        String pin = client.getPin();
        if (!(pin.matches("[0-9]+") && pin.length() == 4))
        {
            throw new InvalidParameterException("PIN should consist of 4 digits.");
        }

        if (Clients.get().containsKey(number)) {
            throw new InvalidParameterException("Client with this number already exists.");
        }
    }


    private void validateClientNumber(String clientNumber) {
        if (!Clients.get().containsKey(clientNumber)) {
            throw new InvalidParameterException("No client with number " + clientNumber);
        }
    }

    private void addPasswordLists(Client client) {
        RandomStringGenerator randomStringGenerator =
                new RandomStringGenerator.Builder()
                        .withinRange('0', 'z')
                        .filteredBy(CharacterPredicates.LETTERS, CharacterPredicates.DIGITS)
                        .build();
        List<String> activePasswords = new ArrayList<>();
        List<String> inactivePasswords = new ArrayList<>();
        for (int i = 0; i < OneTimePasswordList.PASSWORDS_PER_LIST; ++i) {
            activePasswords.add(randomStringGenerator.generate(OneTimePasswordList.PASSWORDS_LENGTH));
            inactivePasswords.add(randomStringGenerator.generate(OneTimePasswordList.PASSWORDS_LENGTH));
        }

        client.setActivatedList(new OneTimePasswordList(activePasswords));
        client.setNonactivatedList(new OneTimePasswordList((inactivePasswords)));
    }

    private Operation createClientAddingOperation(long unixTimestamp, Client client) {
        List<Operation> operations = Operations.get();
        Operation lastOperation = null;
        int number = 0;

        if (!operations.isEmpty()) {
            lastOperation = Operations.get().get(Operations.get().size() - 1);
            number = lastOperation.getNumber();
        }

        Map<String, Object> operationData = clientData(client);
        return new Operation(unixTimestamp, Operation.Type.ADDING_CLIENT, number, operationData, lastOperation);
    }

    private Operation createClientDeletingOperation(long unixTimestamp, Client client) {
        List<Operation> operations = Operations.get();
        Operation lastOperation = null;
        int number = 0;

        if (!operations.isEmpty()) {
            lastOperation = Operations.get().get(Operations.get().size() - 1);
            number = lastOperation.getNumber();
        }

        Map<String, Object> operationData = clientData(client);
        return new Operation(unixTimestamp, Operation.Type.REMOVING_CLIENT, number, operationData, lastOperation);

    }

    private Map<String, Object> clientData(Client client) {
        Map<String, Object> clientData = new HashMap<>();
        clientData.put("number", client.getNumber());
        clientData.put("pin", client.getPin());
        clientData.put("activatedList", client.getActivatedList().getPasswords());
        clientData.put("nonactivatedList", client.getNonactivatedList().getPasswords());
        return clientData;
    }
}