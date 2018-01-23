package com.github.carloszaldivar.distributedauth.communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.data.NeighboursRepository;
import com.github.carloszaldivar.distributedauth.data.OperationsRepository;
import com.github.carloszaldivar.distributedauth.models.FatRequest;
import com.github.carloszaldivar.distributedauth.models.FatRequestResponse;
import com.github.carloszaldivar.distributedauth.models.Neighbour;
import com.github.carloszaldivar.distributedauth.models.Operation;
import com.google.common.collect.ImmutableList;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FatRequestsSender {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.communication.FatRequestsSender");

    private NeighboursRepository neighboursRepository;
    private OperationsRepository operationsRepository;

    public FatRequestsSender(NeighboursRepository neighboursRepository, OperationsRepository operationsRepository) {
        this.neighboursRepository = neighboursRepository;
        this.operationsRepository = operationsRepository;
    }

    public void sendFatRequests() {
        for (Neighbour neighbour : neighboursRepository.getNeighbours().values()) {
            sendFatRequest(neighbour);
        }
    }

    private void handleFatRequestResponse(Neighbour neighbour, List<Operation> historyDifferece, FatRequestResponse fatRequestResponse) {
        if (fatRequestResponse.getRequestTimestamp() < DistributedAuthApplication.getLastConflictResolution()) {
            return;
        }

        switch (fatRequestResponse.getStatus()) {
            case OK:
                long neighbourSyncTime = historyDifferece.get(historyDifferece.size() - 1).getTimestamp();
                updateSyncTimes(fatRequestResponse.getSyncTimes(), neighbour.getId(), neighbourSyncTime);
                DistributedAuthApplication.updateState();
                break;
            case U2OLD:
                DistributedAuthApplication.setState(DistributedAuthApplication.State.TOO_OLD);
                break;
            case CONFLICT:
                DistributedAuthApplication.setState(DistributedAuthApplication.State.CONFLICT);
                break;
        }
    }

    private void updateSyncTimes(Map<String, Long> syncTimes, String senderId, long sendersLastTimestamp) {
        neighboursRepository.updateSyncTime(senderId, sendersLastTimestamp);
        Map<String, Long> savedSyncTimes = neighboursRepository.getSyncTimes();
        for (Map.Entry<String, Long> syncTime : syncTimes.entrySet()) {
            String neighbourId = syncTime.getKey();
            if (savedSyncTimes.containsKey(neighbourId) &&
                    savedSyncTimes.get(neighbourId) < syncTime.getValue()) {
                neighboursRepository.updateSyncTime(neighbourId, syncTime.getValue());
            }
        }
    }

    private HttpPost createHttpRequest(FatRequest fatRequest, Neighbour neighbour) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFatRequest;
        try {
            jsonFatRequest = mapper.writeValueAsString(fatRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HttpPost httpRequest = new HttpPost(String.format("http://%s/private/synchro/fat", neighbour.getUrl()));
        httpRequest.setHeader("Content-Type", "application/json");
        try {
            httpRequest.setEntity(new StringEntity(jsonFatRequest));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        int timeout = 30 * 1000; // Timeout in millis.
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout)
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();

        httpRequest.setConfig(requestConfig);

        return httpRequest;
    }

    private List<Operation> getHistoryDifference(Neighbour neighbour) {
        long syncTime = neighboursRepository.getSyncTimes().get(neighbour.getId());
        int i = 0;
        ImmutableList<Operation> operations = operationsRepository.getAll();
        if (syncTime < operations.get(0).getTimestamp()) {
            return operations.subList(0, operations.size());
        }

        while (operations.get(i).getTimestamp() != syncTime) {
            ++i;
        }
        return operations.size() > (i + 1) ? operations.subList(i + 1, operations.size()) : new ArrayList<>();
    }

    public void sendFatRequest(Neighbour neighbour) {
        List<Operation> historyDifference = getHistoryDifference(neighbour);
        if (historyDifference.isEmpty()) {
            return;
        }

        logger.info(String.format("Sending FatRequest to %s with URL %s", neighbour.getId(), neighbour.getUrl()));
        FatRequest fatRequest = new FatRequest(DistributedAuthApplication.getInstanceName(), historyDifference,
                neighboursRepository.getSyncTimes(), System.currentTimeMillis());

        CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
        client.start();

        HttpPost httpRequest = createHttpRequest(fatRequest, neighbour);

        client.execute(httpRequest, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                String responseJson;
                try {
                    responseJson = EntityUtils.toString(result.getEntity());
                } catch (IOException e) {
                    return;
                }

                FatRequestResponse fatRequestResponse;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    fatRequestResponse = mapper.readValue(responseJson, FatRequestResponse.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                logger.info(String.format("Got response to FatRequest to %s. Response status: %s", neighbour.getId(), fatRequestResponse.getStatus()));
                handleFatRequestResponse(neighbour, historyDifference, fatRequestResponse);
            }

            @Override
            public void failed(Exception ex) {
                logger.info(String.format("Failed to get response from %s to our FatRequest.", neighbour.getId()));
                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void cancelled() {
                logger.info((String.format("FatRequest to %s canceled.", neighbour.getId())));
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
