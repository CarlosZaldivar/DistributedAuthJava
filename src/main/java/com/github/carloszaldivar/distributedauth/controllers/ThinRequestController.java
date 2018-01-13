package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.data.Operations;
import com.github.carloszaldivar.distributedauth.models.Operation;
import com.github.carloszaldivar.distributedauth.models.ThinRequest;
import com.github.carloszaldivar.distributedauth.models.ThinRequestResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

public class ThinRequestController {
    @RequestMapping(method=POST, value={"/thin"})
    public ThinRequestResponse handleThinRequest(@RequestBody ThinRequest thinRequest) {
        List<Operation> localHistory = Operations.get();

        if (localHistory.isEmpty()) {
            return new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NEEDED, Neighbours.getSyncTimes());
        }

        if (thinRequest.getHash().equals(localHistory.get(localHistory.size() - 1).getHash())) {
            updateSyncTimes(thinRequest.getSyncTimes(), thinRequest.getSenderId(), localHistory.get(localHistory.size() - 1).getTimestamp());
            return new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NOT_NEEDED, Neighbours.getSyncTimes());
        } else {
            return new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NEEDED, Neighbours.getSyncTimes());
        }
    }

    /**
     * @param senderId Id of the neighbour that sent the request. It's needed because it may be unavailable in syncTimes.
     * @param neighboursLastTimestamp Timestamp of the last operation that the neighbour we're synchronizing with has.
     *                                It's needed because it may be unavailable in syncTimes.
     */
    private void updateSyncTimes(Map<String, Long> syncTimes, String senderId, long neighboursLastTimestamp) {
        Neighbours.getSyncTimes().put(senderId, neighboursLastTimestamp);

        for (Map.Entry<String, Long> syncTime : syncTimes.entrySet()) {
            String neighbourId = syncTime.getKey();
            if (Neighbours.getSyncTimes().get(neighbourId) < syncTime.getValue()) {
                Neighbours.getSyncTimes().put(neighbourId, syncTime.getValue());
            }
        }
    }
}
