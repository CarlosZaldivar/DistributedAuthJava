package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.data.NeighboursRepository;
import com.github.carloszaldivar.distributedauth.data.OperationsRepository;
import com.github.carloszaldivar.distributedauth.models.Operation;
import com.github.carloszaldivar.distributedauth.models.ThinRequest;
import com.github.carloszaldivar.distributedauth.models.ThinRequestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ThinRequestController {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.controllers.ThinRequestController");

    @Autowired
    private NeighboursRepository neighboursRepository;
    @Autowired
    private OperationsRepository operationsRepository;

    public ThinRequestController(NeighboursRepository neighboursRepository, OperationsRepository operationsRepository) {
        this.neighboursRepository = neighboursRepository;
        this.operationsRepository = operationsRepository;
    }

    @RequestMapping(method=POST, value={"/private/synchro/thin"})
    public ResponseEntity<ThinRequestResponse> handleThinRequest(@RequestBody ThinRequest thinRequest) {
        logger.info("Received ThinRequest from " + thinRequest.getSenderId());
        Operation lastOperation = operationsRepository.getLast();

        ThinRequestResponse response;
        if (lastOperation == null) {
            logger.info("Update needed. Asking sender to send FatRequest.");
            response =  new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NEEDED, neighboursRepository.getSyncTimes(), thinRequest.getTimestamp());
        } else if (thinRequest.getHash().equals(lastOperation.getHash())) {
            logger.info("Update not needed.");
            updateSyncTimes(thinRequest.getSyncTimes(), thinRequest.getSenderId(), lastOperation.getTimestamp());
            response = new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NOT_NEEDED, neighboursRepository.getSyncTimes(), thinRequest.getTimestamp());
        } else {
            logger.info("Update needed. Asking sender to send FatRequest.");
            response = new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NEEDED, neighboursRepository.getSyncTimes(), thinRequest.getTimestamp());
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
        if (neighboursRepository.getNeighbours().containsKey(senderId)) {
            neighboursRepository.updateSyncTime(senderId, sendersLastTimestamp);
        }

        Map<String, Long> savedSyncTimes = neighboursRepository.getSyncTimes();
        for (Map.Entry<String, Long> syncTime : syncTimes.entrySet()) {
            String neighbourId = syncTime.getKey();
            if (savedSyncTimes.containsKey(neighbourId) &&
                    savedSyncTimes.get(neighbourId) < syncTime.getValue()) {
                neighboursRepository.updateSyncTime(neighbourId, syncTime.getValue());
            }
        }
    }
}
