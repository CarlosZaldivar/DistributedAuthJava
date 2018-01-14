package com.github.carloszaldivar.distributedauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.data.Operations;
import com.github.carloszaldivar.distributedauth.models.FatRequest;
import com.github.carloszaldivar.distributedauth.models.FatRequestResponse;
import com.github.carloszaldivar.distributedauth.models.Neighbour;
import com.github.carloszaldivar.distributedauth.models.Operation;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class Synchronizer {
    public void sendFatRequests() {
        for (Neighbour neighbour : Neighbours.get()) {
            List<Operation> historyDifference = getHistoryDifference(neighbour);
            FatRequest fatRequest = new FatRequest(DistributedAuthApplication.getInstanceName(), historyDifference, Neighbours.getSyncTimes());

            CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
            client.start();

            HttpPost httpRequest = createHttpRequest(fatRequest, neighbour);

            client.execute(httpRequest, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    String responseJson;
                    try {
                        responseJson = EntityUtils.toString(result.getEntity());
                        System.out.println(responseJson);
                    } catch (IOException e) {
                        return;
                    }
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        FatRequestResponse fatRequestResponse = mapper.readValue(responseJson, FatRequestResponse.class);
                        handleFastRequestResponse(neighbour, fatRequestResponse);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        client.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void failed(Exception ex) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void cancelled() {
                    System.out.println("Canceled");
                    try {
                        client.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    throw new RuntimeException("Request canceled");
                }
            });
        }
    }

    private void handleFastRequestResponse(Neighbour neighbour, FatRequestResponse fatRequestResponse) {
        // TODO
    }

    private HttpPost createHttpRequest(FatRequest fatRequest, Neighbour neighbour) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFatRequest;
        try {
            jsonFatRequest = mapper.writeValueAsString(fatRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HttpPost httpRequest = new HttpPost(neighbour.getUrl() + "/fat");
        httpRequest.setHeader("Content-Type", "application/json");
        try {
            httpRequest.setEntity(new StringEntity(jsonFatRequest));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return httpRequest;
    }

    private List<Operation> getHistoryDifference(Neighbour neighbour) {
        long syncTime = Neighbours.getSyncTimes().get(neighbour.getId());
        int i = 0;
        List<Operation> operations = Operations.get();
        if (syncTime < operations.get(0).getTimestamp()) {
            return operations.subList(0, operations.size());
        }

        while (operations.get(i).getTimestamp() != syncTime) {
            ++i;
        }
        return operations.size() > (i + 1) ? operations.subList(i + 1, operations.size()) : new ArrayList<>();
    }
}
