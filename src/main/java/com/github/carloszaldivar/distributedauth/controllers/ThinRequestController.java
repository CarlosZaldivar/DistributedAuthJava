package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.data.Operations;
import com.github.carloszaldivar.distributedauth.models.Operation;
import com.github.carloszaldivar.distributedauth.models.ThinRequest;
import com.github.carloszaldivar.distributedauth.models.ThinRequestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ThinRequestController {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.controllers.ThinRequestController");

    @RequestMapping(method=POST, value={"/thin"})
    public ResponseEntity<ThinRequestResponse> handleThinRequest(@RequestBody ThinRequest thinRequest) {
        logger.info("Received ThinRequest from " + thinRequest.getSenderId());
        List<Operation> localHistory = Operations.get();

        ThinRequestResponse response;
        if (localHistory.isEmpty()) {
            logger.info("Update needed. Asking sender to send FatRequest.");
            response =  new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NEEDED, Neighbours.getSyncTimes());
        } else if (thinRequest.getHash().equals(localHistory.get(localHistory.size() - 1).getHash())) {
            logger.info("Update not needed.");
            updateSyncTimes(thinRequest.getSyncTimes(), thinRequest.getSenderId(), localHistory.get(localHistory.size() - 1).getTimestamp());
            response = new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NOT_NEEDED, Neighbours.getSyncTimes());
        } else {
            logger.info("Update needed. Asking sender to send FatRequest.");
            response = new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NEEDED, Neighbours.getSyncTimes());
        }

        DistributedAuthApplication.updateState();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * @param senderId Id of the neighbour that sent the request. It's needed because it may be unavailable in syncTimes.
     * @param sendersLastTimestamp Timestamp of the last operation that the neighbour we're synchronizing with has.
     *                                It's needed because it may be unavailable in syncTimes.
     */
    private void updateSyncTimes(Map<String, Long> syncTimes, String senderId, long sendersLastTimestamp) {
        Neighbours.getSyncTimes().put(senderId, sendersLastTimestamp);

        for (Map.Entry<String, Long> syncTime : syncTimes.entrySet()) {
            String neighbourId = syncTime.getKey();
            if (Neighbours.getSyncTimes().containsKey(neighbourId) &&
                    Neighbours.getSyncTimes().get(neighbourId) < syncTime.getValue()) {
                Neighbours.getSyncTimes().put(neighbourId, syncTime.getValue());
            }
        }
    }
}
