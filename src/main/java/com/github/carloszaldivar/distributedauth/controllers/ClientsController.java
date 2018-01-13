package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.models.Client;
import com.github.carloszaldivar.distributedauth.data.Clients;
import com.github.carloszaldivar.distributedauth.models.Operation;
import com.github.carloszaldivar.distributedauth.data.Operations;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ClientsController {

    @RequestMapping(method=POST, value={"/clients"})
    public Client create(@RequestBody Client client) {
        validateClient(client);
        Clients.get().put(client.getNumber(), client);
        Operation addingClientOperation = createClientAddingOperation(System.currentTimeMillis(), client);
        Operations.get().add(addingClientOperation);
        return client;
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

    @ExceptionHandler(InvalidParameterException.class)
    private Map<String, String> handleException(InvalidParameterException exception) {
        Map<String, String> error = new HashMap<>();
        error.put("error", exception.getMessage());
        return error;
    }

    private Operation createClientAddingOperation(long unixTimestamp, Client client) {
        List<Operation> operations = Operations.get();
        Operation lastOperation = null;
        int number = 0;

        if (!operations.isEmpty()) {
            lastOperation = Operations.get().get(Operations.get().size() - 1);
            number = lastOperation.getNumber();
        }

        Map<String, Object> operationData = new HashMap<>();
        operationData.put("number", client.getNumber());
        operationData.put("pin", client.getPin());

        return new Operation(unixTimestamp, Operation.Type.ADDING_CLIENT, number, operationData, lastOperation);
    }
}