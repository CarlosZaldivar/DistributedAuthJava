package com.github.carloszaldivar.distributedauth.communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.carloszaldivar.distributedauth.models.AuthorizationRequest;
import com.github.carloszaldivar.distributedauth.models.Neighbour;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class AuthRequestsSender {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.communication.AuthRequestsSender");

    public boolean neighbourAgreesToAuthenticate(Neighbour specialNeighbour, String clientNumber, String pin) {
        logger.info(String.format("Sending authentication request to special neighbour %s with URL %s", specialNeighbour.getId(), specialNeighbour.getUrl()));
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost request = createAuthenticationHttpRequest(specialNeighbour, clientNumber, pin);
            HttpResponse response = client.execute(request);
            boolean authenticated = response.getStatusLine().getStatusCode() == HttpStatus.OK.value();
            logger.info("Authentication result - " + authenticated);
            return authenticated;
        } catch (IOException e) {
            return true;
        }
    }

    public boolean neighbourAgreesToAuthorizeOperation(Neighbour specialNeighbour, String clientNumber, AuthorizationRequest authorizationRequest) {
        logger.info(String.format("Sending operation authorization request to special neighbour %s with URL %s", specialNeighbour.getId(), specialNeighbour.getUrl()));
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost request = createOperationAuthorizationHttpRequest(specialNeighbour, clientNumber, authorizationRequest);
            HttpResponse response = client.execute(request);
            boolean authorized = response.getStatusLine().getStatusCode() == HttpStatus.OK.value();
            logger.info("Authorization result - " + authorized);
            return authorized;
        } catch (IOException e) {
            return true;
        }
    }

    public boolean neighbourAgreesToActivateNewPasswordList(Neighbour specialNeighbour, String clientNumber, AuthorizationRequest authorizationRequest) {
        logger.info(String.format("Sending new list activation authorization request to special neighbour %s with URL %s", specialNeighbour.getId(), specialNeighbour.getUrl()));
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost request = createNewPasswordListAuthorizationHttpRequest(specialNeighbour, clientNumber, authorizationRequest);
            HttpResponse response = client.execute(request);
            boolean authorized = response.getStatusLine().getStatusCode() == HttpStatus.OK.value();
            logger.info("Authorization result - " + authorized);
            return authorized;
        } catch (IOException e) {
            return true;
        }
    }

    private HttpPost createAuthenticationHttpRequest(Neighbour specialNeighbour, String clientNumber, String pin) {
        String url = String.format("%s/clients/%s/authenticate", specialNeighbour.getUrl(), clientNumber);
        return createHttpRequest(url, pin);
    }


    private HttpPost createOperationAuthorizationHttpRequest(Neighbour specialNeighbour, String clientNumber, AuthorizationRequest authorizationRequest) {
        ObjectMapper mapper = new ObjectMapper();
        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(authorizationRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String url = String.format("%s/clients/%s/softauthorize/operation", specialNeighbour.getUrl(), clientNumber);
        return createHttpRequest(url, requestBody);
    }

    private HttpPost createNewPasswordListAuthorizationHttpRequest(Neighbour specialNeighbour, String clientNumber, AuthorizationRequest authorizationRequest) {
        String url = String.format("%s/clients/%s/softauthorize/list", specialNeighbour.getUrl(), clientNumber);
        ObjectMapper mapper = new ObjectMapper();
        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(authorizationRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return createHttpRequest(url, requestBody);
    }

    private HttpPost createHttpRequest(String url, String requestBody) {
        HttpPost httpRequest = new HttpPost(url);
        httpRequest.setHeader("Content-Type", "application/json");
        try {
            httpRequest.setEntity(new StringEntity(requestBody));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        int timeout = 5 * 1000; // Timeout in millis.
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout)
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();

        httpRequest.setConfig(requestConfig);
        return httpRequest;
    }
}
