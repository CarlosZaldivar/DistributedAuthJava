package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.data.ClientsRepository;
import com.github.carloszaldivar.distributedauth.models.AuthorizationRequest;
import com.github.carloszaldivar.distributedauth.models.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class SoftAuthorizationController {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.controllers.SoftAuthorizationController");

    @Autowired
    private ClientsRepository clientsRepository;

    public SoftAuthorizationController(ClientsRepository clientsRepository) {
        this.clientsRepository = clientsRepository;
    }

    @RequestMapping(method=POST, value={"/public/clients/{id}/checkpass"})
    public ResponseEntity authorizeOperation(@PathVariable(value="id") String clientNumber, @RequestBody AuthorizationRequest request)
    {
        logger.info("Received checkpass request.");
        validateClientNumber(clientNumber);
        validateAuthorizationData(request);
        Client client = clientsRepository.get(clientNumber);
        int passwordIndex = client.getActivatedList().getCurrentIndex();
        HttpStatus status = request.getPin().equals(client.getPin()) && client.getActivatedList().getPasswords().get(passwordIndex).equals(request.getOneTimePassword()) ?
                HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        logger.info("Checkpass response - " + status);
        return new ResponseEntity(status);
    }

    @RequestMapping(method=POST, value={"/clients/{id}/softauthorize/list"})
    public ResponseEntity activateNewPasswordList(@PathVariable(value="id") String clientNumber, @RequestBody AuthorizationRequest request) {
        logger.info("Received soft new list activation request.");
        validateClientNumber(clientNumber);
        validateAuthorizationData(request);
        Client clientCopy = new Client(clientsRepository.get(clientNumber));
        HttpStatus status = request.getPin().equals(clientCopy.getPin()) && clientCopy.activateNewOneTimePasswordList(request.getOneTimePassword()) ?
                HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        logger.info("Soft new list activation request response - " + status);
        return new ResponseEntity(status);
    }

    private void validateAuthorizationData(AuthorizationRequest request) {
        if (request.getPin() == null || request.getOneTimePassword() == null) {
            throw new IllegalArgumentException("PIN and password should be provided.");
        }
    }

    private void validateClientNumber(String clientNumber) {
        if (clientsRepository.get(clientNumber) == null) {
            throw new IllegalArgumentException("No client with number " + clientNumber);
        }
    }
}
