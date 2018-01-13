package com.github.carloszaldivar.distributedauth.controllers;

import com.github.carloszaldivar.distributedauth.data.Clients;
import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.models.*;
import com.github.carloszaldivar.distributedauth.data.Operations;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

public class SynchronizationController {
    private List<Operation> localHistory;
    private List<Operation> neighbourHistory;
    private FatRequest fatRequest;

    @RequestMapping(method=POST, value={"/fat"})
    public FatRequestResponse handleFatRequest(@RequestBody FatRequest fatRequest) {
        neighbourHistory = fatRequest.getHistory();
        localHistory = Operations.get();
        this.fatRequest = fatRequest;

        if (localHistory.isEmpty()) {
            return handleEmptyLocalHistory();
        }

        if (sameHistory()) {
            return handleSameHistory();
        }

        DivergencePoint divergencePoint = findDivergencePoint(localHistory, neighbourHistory);

        switch (divergencePoint.getType()) {
            case LOCAL_TOO_OLD:
                return handleLocalTooOld(divergencePoint);
            case NEIGHBOUR_TOO_OLD:
                return new FatRequestResponse(FatRequestResponse.Status.U2OLD, Neighbours.getSyncTimes());
            case LOCAL_NOT_CORRECT:
                return handleLocalNotCorrect(divergencePoint);
            case NEIGHBOUR_NOT_CORRECT:
                return new FatRequestResponse(FatRequestResponse.Status.CONFLICT, Neighbours.getSyncTimes());
            default:
                throw new RuntimeException("Unexpected divergence point.");
        }
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

    private FatRequestResponse handleLocalTooOld(DivergencePoint divergencePoint) {
        List<Operation> historyDifference = neighbourHistory.subList(divergencePoint.getNeighbourHistoryIndex(), neighbourHistory.size());
        localHistory.addAll(historyDifference);
        apply(historyDifference);
        updateSyncTimes();
        return new FatRequestResponse(FatRequestResponse.Status.OK, Neighbours.getSyncTimes());
    }

    private FatRequestResponse handleLocalNotCorrect(DivergencePoint divergencePoint) {
        List<Operation> historyToRemove = localHistory.subList(divergencePoint.getLocalHistoryIndex(), localHistory.size());
        unapply(historyToRemove);
        localHistory.removeAll(historyToRemove);
        List<Operation> historyToAdd = neighbourHistory.subList(divergencePoint.getNeighbourHistoryIndex(), neighbourHistory.size());
        apply(historyToAdd);
        localHistory.addAll(historyToAdd);
        updateSyncTimes();
        return new FatRequestResponse(FatRequestResponse.Status.OK, Neighbours.getSyncTimes());
    }

    private FatRequestResponse handleSameHistory() {
        updateSyncTimes();
        return new FatRequestResponse(FatRequestResponse.Status.OK, Neighbours.getSyncTimes());
    }

    private boolean sameHistory() {
        Operation localLastOperation = localHistory.get(localHistory.size() -1);
        Operation neighboursLastOperation = neighbourHistory.get(neighbourHistory.size() - 1);
        return localLastOperation.getHash().equals(neighboursLastOperation.getHash());
    }

    private FatRequestResponse handleEmptyLocalHistory() {
        localHistory.addAll(neighbourHistory);
        apply(neighbourHistory);
        updateSyncTimes();
        return new FatRequestResponse(FatRequestResponse.Status.OK, Neighbours.getSyncTimes());
    }

    private void updateSyncTimes() {
        Operation neighboursLastOperation = neighbourHistory.get(neighbourHistory.size() - 1);
        Neighbours.getSyncTimes().put(fatRequest.getSenderId(), neighboursLastOperation.getTimestamp());

        // TODO updating syncTimes for the rest of the neighbours. May involve putting earlier timestamps if there were conflicts.
    }

    private void apply(List<Operation> operations) {
        for (Operation operation : operations) {
            switch (operation.getType()) {
                case ADDING_CLIENT:
                    Map<String, Object> data = operation.getData();
                    Client client = new Client((String) data.get("number"), (String) data.get("pin"));
                    Clients.get().put(client.getNumber(), client);
            }
        }
    }

    private void unapply(List<Operation> operations) {
        for (Operation operation : operations) {
            switch (operation.getType()) {
                case ADDING_CLIENT:
                    String clientNumber = (String) operation.getData().get("number");
                    Clients.get().remove(clientNumber);
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

    @RequestMapping(method=POST, value={"/thin"})
    public ThinRequestResponse handleThinRequest(@RequestBody ThinRequest thinRequest) {
        localHistory = Operations.get();

        if (localHistory.isEmpty()) {
            return new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NEEDED, Neighbours.getSyncTimes());
        }

        if (thinRequest.getHash().equals(localHistory.get(localHistory.size() - 1).getHash())) {
            updateSyncTimes();
            return new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NOT_NEEDED, Neighbours.getSyncTimes());
        } else {
            return new ThinRequestResponse(ThinRequestResponse.Status.UPDATE_NEEDED, Neighbours.getSyncTimes());
        }
    }
}
