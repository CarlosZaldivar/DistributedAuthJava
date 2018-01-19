package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.DistributedAuthApplication;
import com.github.carloszaldivar.distributedauth.data.*;
import com.github.carloszaldivar.distributedauth.models.*;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private ImmutableList<Operation> localHistory;
    private List<Operation> neighbourHistory;
    private FatRequest fatRequest;

    private String serverName = DistributedAuthApplication.getInstanceName();

    @Autowired
    private NeighboursRepository neighboursRepository;
    @Autowired
    private OperationsRepository operationsRepository;
    @Autowired
    private ClientsRepository clientsRepository;

    public FatRequestController(NeighboursRepository neighboursRepository, ClientsRepository clientsRepository, OperationsRepository operationsRepository) {
        this.neighboursRepository = neighboursRepository;
        this.clientsRepository = clientsRepository;
        this.operationsRepository = operationsRepository;
    }

    @RequestMapping(method=POST, value={"/private/synchro/fat"})
    public ResponseEntity<FatRequestResponse> handleFatRequest(@RequestBody FatRequest fatRequest) {
        validateFatRequest(fatRequest);
        logger.info("Got FatRequest from " + fatRequest.getSenderId());
        neighbourHistory = fatRequest.getHistory();
        operationsRepository.lockWrite();
        try {
            localHistory = operationsRepository.getAll();
            this.fatRequest = fatRequest;

            FatRequestResponse response;
            if (localHistory.isEmpty()) {
                logger.info("Local history was empty. Updating.");
                response = handleEmptyLocalHistory(fatRequest.getTimestamp());
            } else if (sameHistory()) {
                logger.info("Same histories. No need to update.");
                response = handleSameHistory(fatRequest.getTimestamp());
            } else {
                response = handleDifferentHistories(fatRequest.getTimestamp());
            }

            DistributedAuthApplication.updateState();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } finally {
            operationsRepository.unlockWrite();
        }
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
                return new FatRequestResponse(serverName, FatRequestResponse.Status.U2OLD, neighboursRepository.getSyncTimes(), requestTimestamp,
                        operationsRepository.getLast().getTimestamp());
            case LOCAL_NOT_CORRECT:
                logger.info("Local history was incorrect. Fixing it.");
                return handleLocalNotCorrect(divergencePoint, requestTimestamp);
            case NEIGHBOUR_NOT_CORRECT:
                logger.info("Sender's history was incorrect. Informing him about it.");
                return new FatRequestResponse(serverName, FatRequestResponse.Status.CONFLICT, neighboursRepository.getSyncTimes(), requestTimestamp,
                        operationsRepository.getLast().getTimestamp());
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
                return new DivergencePoint(localHistory.size() - 1, 0, DivergencePoint.Type.NEIGHBOUR_NOT_CORRECT);
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
        operationsRepository.addToEnd(historyDifference);
        apply(historyDifference);
        updateSyncTimes(fatRequest.getSyncTimes());
        DistributedAuthApplication.setState(DistributedAuthApplication.State.UNSYNCHRONIZED);
        return new FatRequestResponse(serverName, FatRequestResponse.Status.OK,
                neighboursRepository.getSyncTimes(), requestTimesamp, operationsRepository.getLast().getTimestamp());
    }

    private FatRequestResponse handleLocalNotCorrect(DivergencePoint divergencePoint, long requestTimestamp) {
        int localHistoryIndex = divergencePoint.getLocalHistoryIndex();
        List<Operation> historyToRemove = localHistory.subList(localHistoryIndex, localHistory.size());
        unapply(historyToRemove);
        operationsRepository.removeFromEnd(historyToRemove.size());
        long lastCorrectTimestamp = localHistoryIndex > 0 ? localHistory.get(localHistoryIndex - 1).getTimestamp() : 0;
        downgradeSyncTimes(lastCorrectTimestamp);

        List<Operation> historyToAdd = neighbourHistory.subList(divergencePoint.getNeighbourHistoryIndex(), neighbourHistory.size());
        apply(historyToAdd);
        operationsRepository.addToEnd(historyToAdd);

        updateSyncTimes(fatRequest.getSyncTimes());
        DistributedAuthApplication.setState(DistributedAuthApplication.State.UNSYNCHRONIZED);
        DistributedAuthApplication.setLastConflictResolution(requestTimestamp);
        return new FatRequestResponse(serverName, FatRequestResponse.Status.FIXED, neighboursRepository.getSyncTimes(), requestTimestamp,
                operationsRepository.getLast().getTimestamp());
    }

    private FatRequestResponse handleSameHistory(long requestTimestamp) {
        updateSyncTimes(fatRequest.getSyncTimes());
        return new FatRequestResponse(serverName, FatRequestResponse.Status.ALREADY_SYNC, neighboursRepository.getSyncTimes(), requestTimestamp,
                operationsRepository.getLast().getTimestamp());
    }

    private boolean sameHistory() {
        Operation localLastOperation = localHistory.get(localHistory.size() -1);
        Operation neighboursLastOperation = neighbourHistory.get(neighbourHistory.size() - 1);
        return localLastOperation.getHash().equals(neighboursLastOperation.getHash());
    }

    private FatRequestResponse handleEmptyLocalHistory(long requestTimestamp) {
        operationsRepository.addToEnd(neighbourHistory);
        apply(neighbourHistory);
        updateSyncTimes(fatRequest.getSyncTimes());
        return new FatRequestResponse(serverName, FatRequestResponse.Status.OK, neighboursRepository.getSyncTimes(), requestTimestamp,
                operationsRepository.getLast().getTimestamp());
    }

    private void downgradeSyncTimes(long lastCorrectTimestamp) {
        Map<String, Long> syncTimes = neighboursRepository.getSyncTimes();
        for (Map.Entry<String, Long> syncTime : syncTimes.entrySet()) {
            if (syncTime.getValue() > lastCorrectTimestamp) {
                neighboursRepository.updateSyncTime(syncTime.getKey(), lastCorrectTimestamp);
            }
        }
    }

    private void updateSyncTimes(Map<String, Long> syncTimes) {
        long neighboursLastTimestamp = neighbourHistory.get(neighbourHistory.size() - 1).getTimestamp();
        if (neighboursRepository.getNeighbours().containsKey(fatRequest.getSenderId())) {
            neighboursRepository.updateSyncTime(fatRequest.getSenderId(), neighboursLastTimestamp);
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

    private void apply(List<Operation> operations) {
        for (Operation operation : operations) {
            switch (operation.getType()) {
                case ADD_CLIENT: {
                    Client newClient = operation.getClientAfter();
                    clientsRepository.add(new Client(newClient));
                    break;
                }
                case DELETE_CLIENT: {
                    clientsRepository.delete(operation.getClientBefore().getNumber());
                    break;
                }
                case AUTHORIZATION: {
                    Client client = operation.getClientAfter();
                    clientsRepository.update(new Client(client));
                    break;
                }
                case LIST_ACTIVATION: {
                    Client client = operation.getClientAfter();
                    clientsRepository.update(new Client(client));
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
                case ADD_CLIENT: {
                    String clientNumber = operation.getClientAfter().getNumber();
                    clientsRepository.delete(clientNumber);
                    break;
                }
                case DELETE_CLIENT: {
                    Client client = operation.getClientBefore();
                    clientsRepository.add(new Client(client));
                    break;
                }
                case AUTHORIZATION: {
                    Client client = operation.getClientBefore();
                    clientsRepository.update(new Client(client));
                    break;
                }
                case LIST_ACTIVATION: {
                    Client client = operation.getClientBefore();
                    clientsRepository.update(new Client(client));
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
