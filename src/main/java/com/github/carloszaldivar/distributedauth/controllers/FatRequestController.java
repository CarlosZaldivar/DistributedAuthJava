package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.data.Clients;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.models.*;
import com.github.carloszaldivar.distributedauth.data.Operations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class FatRequestController {
    private Logger logger = LoggerFactory.getLogger("com.github.carloszaldivar.distributedauth.controllers.FatRequestsController");

    private List<Operation> localHistory;
    private List<Operation> neighbourHistory;
    private FatRequest fatRequest;

    @RequestMapping(method=POST, value={"/fat"})
    public ResponseEntity<FatRequestResponse> handleFatRequest(@RequestBody FatRequest fatRequest) {
        validateFatRequest(fatRequest);
        logger.info("Got FatRequest from " + fatRequest.getSenderId());
        neighbourHistory = fatRequest.getHistory();
        localHistory = Operations.get();
        this.fatRequest = fatRequest;

        FatRequestResponse response;
        if (localHistory.isEmpty()) {
            response = handleEmptyLocalHistory(fatRequest.getTimestamp());
        } else if (sameHistory()) {
            response = handleSameHistory(fatRequest.getTimestamp());
        } else {
            response = handleDifferentHistories(fatRequest.getTimestamp());
        }

        DistributedAuthApplication.updateState();
        HttpStatus httpStatus = response.getStatus() == FatRequestResponse.Status.OK ? HttpStatus.OK : HttpStatus.CONFLICT;
        return new ResponseEntity<>(response, httpStatus);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    private ResponseEntity<String> handleException(IllegalArgumentException exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private FatRequestResponse handleDifferentHistories(long requestTimestamp) {
        DivergencePoint divergencePoint = findDivergencePoint(localHistory, neighbourHistory);

        switch (divergencePoint.getType()) {
            case LOCAL_TOO_OLD:
                logger.info("Local history was not up to date. Updating.");
                return handleLocalTooOld(divergencePoint, requestTimestamp);
            case NEIGHBOUR_TOO_OLD:
                logger.info("Sender's history was not up to date. Informing him about it.");
                return new FatRequestResponse(FatRequestResponse.Status.U2OLD, Neighbours.getSyncTimes(), requestTimestamp);
            case LOCAL_NOT_CORRECT:
                logger.info("Local history was incorrect. Fixing it.");
                return handleLocalNotCorrect(divergencePoint, requestTimestamp);
            case NEIGHBOUR_NOT_CORRECT:
                logger.info("Sender's history was incorrect. Informing him about it.");
                return new FatRequestResponse(FatRequestResponse.Status.CONFLICT, Neighbours.getSyncTimes(), requestTimestamp);
            default:
                throw new RuntimeException("Unexpected divergence point.");
        }
    }

    private void validateFatRequest(FatRequest fatRequest) {
        if (fatRequest == null) {
            throw new IllegalArgumentException("FatRequest is null");
        }
        // TODO Add more checking (low priority)
    }

    private DivergencePoint findDivergencePoint(List<Operation> localHistory, List<Operation> neighbourHistory) {
        Operation firstNeighbourOperation = neighbourHistory.get(0);
        Operation lastLocalOperation = localHistory.get(localHistory.size() - 1);

        if (lastLocalOperation.getTimestamp() < firstNeighbourOperation.getTimestamp()) {
            if (lastLocalOperation.isBefore(firstNeighbourOperation)) {
                return new DivergencePoint(localHistory.size() - 1, 0, DivergencePoint.Type.LOCAL_TOO_OLD);
            } else {
                // It's possible that local history was reverted at some point but the updates that caused the reversion didn't
                // reach the neighbour. In this case neighbour will be sending operations that we already know are incorrect.
                return new DivergencePoint(localHistory.size() - 1, 0, DivergencePoint.Type.NEIGHBOUR_TOO_OLD);
            }
        }

        int localHistoryIndex = 0;
        while (localHistory.get(localHistoryIndex).getNumber() != firstNeighbourOperation.getNumber()) {
            ++localHistoryIndex;
        }

        int neighbourHistoryIndex = 0;
        while (localHistory.get(localHistoryIndex).getHash().equals(neighbourHistory.get(neighbourHistoryIndex).getHash())) {
            ++localHistoryIndex;
            ++neighbourHistoryIndex;

            if (localHistoryIndex == localHistory.size()) {
                return new DivergencePoint(localHistoryIndex, neighbourHistoryIndex, DivergencePoint.Type.LOCAL_TOO_OLD);
            }
            if (neighbourHistoryIndex == neighbourHistory.size()) {
                return new DivergencePoint(localHistoryIndex, neighbourHistoryIndex, DivergencePoint.Type.NEIGHBOUR_TOO_OLD);
            }
        }

        if (localHistory.get(localHistoryIndex).getTimestamp() > neighbourHistory.get(neighbourHistoryIndex).getTimestamp()) {
            return new DivergencePoint(localHistoryIndex, neighbourHistoryIndex, DivergencePoint.Type.LOCAL_NOT_CORRECT);
        } else {
            return new DivergencePoint(localHistoryIndex, neighbourHistoryIndex, DivergencePoint.Type.NEIGHBOUR_NOT_CORRECT);
        }
    }

    private FatRequestResponse handleLocalTooOld(DivergencePoint divergencePoint, long requestTimesamp) {
        List<Operation> historyDifference = neighbourHistory.subList(divergencePoint.getNeighbourHistoryIndex(), neighbourHistory.size());
        localHistory.addAll(historyDifference);
        apply(historyDifference);
        updateSyncTimes(fatRequest.getSyncTimes());
        DistributedAuthApplication.setState(DistributedAuthApplication.State.UNSYNCHRONIZED);
        return new FatRequestResponse(FatRequestResponse.Status.OK, Neighbours.getSyncTimes(), requestTimesamp);
    }

    private FatRequestResponse handleLocalNotCorrect(DivergencePoint divergencePoint, long requestTimestamp) {
        int localHistoryIndex = divergencePoint.getLocalHistoryIndex();
        List<Operation> historyToRemove = localHistory.subList(localHistoryIndex, localHistory.size());
        unapply(historyToRemove);
        localHistory.removeAll(historyToRemove);
        long lastCorrectTimestamp = localHistoryIndex > 0 ? localHistory.get(localHistoryIndex - 1).getTimestamp() : 0;
        downgradeSyncTimes(lastCorrectTimestamp);

        List<Operation> historyToAdd = neighbourHistory.subList(divergencePoint.getNeighbourHistoryIndex(), neighbourHistory.size());
        apply(historyToAdd);
        localHistory.addAll(historyToAdd);

        updateSyncTimes(fatRequest.getSyncTimes());
        DistributedAuthApplication.setState(DistributedAuthApplication.State.UNSYNCHRONIZED);
        DistributedAuthApplication.setLastConflictResolution(requestTimestamp);
        return new FatRequestResponse(FatRequestResponse.Status.OK, Neighbours.getSyncTimes(), requestTimestamp);
    }

    private FatRequestResponse handleSameHistory(long requestTimestamp) {
        updateSyncTimes(fatRequest.getSyncTimes());
        return new FatRequestResponse(FatRequestResponse.Status.OK, Neighbours.getSyncTimes(), requestTimestamp);
    }

    private boolean sameHistory() {
        Operation localLastOperation = localHistory.get(localHistory.size() -1);
        Operation neighboursLastOperation = neighbourHistory.get(neighbourHistory.size() - 1);
        return localLastOperation.getHash().equals(neighboursLastOperation.getHash());
    }

    private FatRequestResponse handleEmptyLocalHistory(long requestTimestamp) {
        localHistory.addAll(neighbourHistory);
        apply(neighbourHistory);
        updateSyncTimes(fatRequest.getSyncTimes());
        return new FatRequestResponse(FatRequestResponse.Status.OK, Neighbours.getSyncTimes(), requestTimestamp);
    }

    private void downgradeSyncTimes(long lastCorrectTimestamp) {
        for (Map.Entry<String, Long> syncTime : Neighbours.getSyncTimes().entrySet()) {
            if (syncTime.getValue() > lastCorrectTimestamp) {
                Neighbours.getSyncTimes().put(syncTime.getKey(), lastCorrectTimestamp);
            }
        }
    }

    private void updateSyncTimes(Map<String, Long> syncTimes) {
        long neighboursLastTimestamp = neighbourHistory.get(neighbourHistory.size() - 1).getTimestamp();
        Neighbours.getSyncTimes().put(fatRequest.getSenderId(), neighboursLastTimestamp);

        for (Map.Entry<String, Long> syncTime : syncTimes.entrySet()) {
            String neighbourId = syncTime.getKey();
            if (Neighbours.getSyncTimes().containsKey(neighbourId) &&
                    Neighbours.getSyncTimes().get(neighbourId) < syncTime.getValue()) {
                Neighbours.getSyncTimes().put(neighbourId, syncTime.getValue());
            }
        }
    }

    private void apply(List<Operation> operations) {
        for (Operation operation : operations) {
            switch (operation.getType()) {
                case ADDING_CLIENT: {
                    Client newClient = operation.getClientAfter();
                    Clients.get().put(newClient.getNumber(), newClient);
                    break;
                }
                case REMOVING_CLIENT: {
                    Client clientToRemove = operation.getClientBefore();
                    Clients.get().remove(clientToRemove.getNumber());
                    break;
                }
                case AUTHORIZATION: {
                    Client client = operation.getClientAfter();
                    Clients.get().put(client.getNumber(), client);
                    break;
                }
                case LIST_ACTIVATION: {
                    Client client = operation.getClientAfter();
                    Clients.get().put(client.getNumber(), client);
                    break;
                }
                default:
                    throw new RuntimeException("Operation type not supported.");
            }
        }
    }

    private void unapply(List<Operation> operations) {
        for (Operation operation : operations) {
            switch (operation.getType()) {
                case ADDING_CLIENT: {
                    String clientNumber = operation.getClientAfter().getNumber();
                    Clients.get().remove(clientNumber);
                    break;
                }
                case REMOVING_CLIENT: {
                    Client client = operation.getClientBefore();
                    Clients.get().put(client.getNumber(), client);
                    break;
                }
                case AUTHORIZATION: {
                    Client client = operation.getClientBefore();
                    Clients.get().put(client.getNumber(), client);
                    break;
                }
                case LIST_ACTIVATION: {
                    Client client = operation.getClientBefore();
                    Clients.get().put(client.getNumber(), client);
                    break;
                }
                default:
                    throw new RuntimeException("Operation type not supported.");
            }
        }
    }

    private static class DivergencePoint {
        private int localHistoryIndex;
        private int neighbourHistoryIndex;
        private Type type;

        public DivergencePoint(int localHistoryIndex, int neighbourHistoryIndex, Type type) {
            this.localHistoryIndex = localHistoryIndex;
            this.neighbourHistoryIndex = neighbourHistoryIndex;
            this.type = type;
        }

        public int getLocalHistoryIndex() {
            return localHistoryIndex;
        }

        public int getNeighbourHistoryIndex() {
            return neighbourHistoryIndex;
        }

        public Type getType() {
            return type;
        }

        public enum Type { LOCAL_TOO_OLD, NEIGHBOUR_TOO_OLD, LOCAL_NOT_CORRECT, NEIGHBOUR_NOT_CORRECT }
    }
}
