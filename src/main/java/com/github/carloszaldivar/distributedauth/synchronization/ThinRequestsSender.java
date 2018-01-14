package com.github.carloszaldivar.distributedauth.synchronization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.data.Operations;
import com.github.carloszaldivar.distributedauth.models.*;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

@Component
public class ThinRequestsSender {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.synchronization.ThinRequestsSender");

    @Scheduled(fixedRate = 1000)
    public void sendThinRequests() {
        if (DistributedAuthApplication.getState() == DistributedAuthApplication.State.SYNCHRONIZED) {
            logger.info("Not sending thin requests - server synchronized.");
            return;
        }

        if (DistributedAuthApplication.getState() == DistributedAuthApplication.State.CONFLICT) {
            logger.info("Not sending thin requests - server has incorrect information.");
            return;
        }

        if (DistributedAuthApplication.getState() == DistributedAuthApplication.State.TOO_OLD) {
            logger.info("Not sending thin requests - server has outdated information.");
            return;
        }

        Operation lastOperation = Operations.get().get(Operations.get().size() - 1);
        Map<String, Long> syncTimes = Neighbours.getSyncTimes();
        for (Neighbour neighbour : Neighbours.get()) {
            if (syncTimes.get(neighbour.getId()) < lastOperation.getTimestamp()) {
                sendThinRequest(neighbour, syncTimes, lastOperation);
                logger.info("Thin request sent to " + neighbour.getId());
            }
        }
    }

    private void sendThinRequest(Neighbour neighbour, Map<String, Long> syncTimes, Operation lastOperation) {
        ThinRequest thinRequest = new ThinRequest(neighbour.getId(), lastOperation.getHash(), syncTimes);
        HttpPost httpRequest = createHttpRequest(thinRequest, neighbour);

        CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
        client.start();
        client.execute(httpRequest, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                String responseJson;
                try {
                    responseJson = EntityUtils.toString(result.getEntity());
                } catch (IOException e) {
                    return;
                }

                ThinRequestResponse thinRequestResponse;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    thinRequestResponse = mapper.readValue(responseJson, ThinRequestResponse.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                logger.info(String.format("Got response to ThinRequest to %s. Response status: %s", neighbour.getId(), thinRequestResponse.getStatus()));
                handleThinRequestResponse(neighbour, lastOperation, thinRequestResponse);
            }

            @Override
            public void failed(Exception ex) {
                logger.info(String.format("Failed to get response from %s to our ThinRequest.", neighbour.getId()));
                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void cancelled() {
                logger.info((String.format("ThinRequest to %s canceled.", neighbour.getId())));
                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                throw new RuntimeException("Request canceled");
            }
        });
    }

    private void handleThinRequestResponse(Neighbour neighbour, Operation lastOperation, ThinRequestResponse thinRequestResponse) {
        switch (thinRequestResponse.getStatus()) {
            case UPDATE_NEEDED:
                (new FatRequestsSender()).sendFatRequest(neighbour);
                break;
            case UPDATE_NOT_NEEDED:
                updateSyncTimes(thinRequestResponse.getSyncTimes(), neighbour.getId(), lastOperation.getTimestamp());
                DistributedAuthApplication.updateState();
                break;
        }
    }

    private void updateSyncTimes(Map<String, Long> syncTimes, String senderId, long sendersLastTimestamp) {
        Neighbours.getSyncTimes().put(senderId, sendersLastTimestamp);
        for (Map.Entry<String, Long> syncTime : syncTimes.entrySet()) {
            String neighbourId = syncTime.getKey();
            if (Neighbours.getSyncTimes().get(neighbourId) < syncTime.getValue()) {
                Neighbours.getSyncTimes().put(neighbourId, syncTime.getValue());
            }
        }
    }

    private HttpPost createHttpRequest(ThinRequest thinRequest, Neighbour neighbour) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonThinRequest;
        try {
            jsonThinRequest = mapper.writeValueAsString(thinRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HttpPost httpRequest = new HttpPost(neighbour.getUrl() + "/thin");
        httpRequest.setHeader("Content-Type", "application/json");
        try {
            httpRequest.setEntity(new StringEntity(jsonThinRequest));
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
}
