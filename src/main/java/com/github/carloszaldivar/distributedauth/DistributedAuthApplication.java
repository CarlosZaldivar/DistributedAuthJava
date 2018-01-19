package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.data.*;
import com.github.carloszaldivar.distributedauth.models.Neighbour;
import com.github.carloszaldivar.distributedauth.models.Operation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@SpringBootApplication
@EnableScheduling
public class DistributedAuthApplication {
	private static String instanceName;
	private static State state = State.SYNCHRONIZED;
    private static long lastConflictResolution = 0;
    private static NeighboursRepository neighboursRepository = new LocalNeighboursRepository();
    private static OperationsRepository operationsRepository = new LocalOperationsRepository();

    public enum State { SYNCHRONIZED, UNSYNCHRONIZED, CONFLICT, TOO_OLD }
    private static boolean historyCleaning = true;

	public static void main(String[] args) {
        if (args.length > 0) {
	        instanceName = args[0];
        } else {
	        instanceName = "test-" + System.currentTimeMillis();
        }
        historyCleaning = false;
        SpringApplication.run(DistributedAuthApplication.class, args);
	}

    /**
     * Change state to SYNCHRONIZED if possible.
     */
	public static void updateState() {
	    operationsRepository.lockWrite();
	    try {
            DistributedAuthApplication.State currentState = DistributedAuthApplication.getState();
            if (currentState == DistributedAuthApplication.State.TOO_OLD || currentState == DistributedAuthApplication.State.CONFLICT) {
                return;
            }

            List<Operation> operations = operationsRepository.getAll();
            if (operations.isEmpty()) {
                state = State.SYNCHRONIZED;
                return;
            }

            long lastTimestamp = operations.get(operations.size() - 1).getTimestamp();
            for (Neighbour neighbour : neighboursRepository.getNeighbours().values()) {
                if (neighboursRepository.getSyncTimes().get(neighbour.getId()) != lastTimestamp) {
                    state = State.UNSYNCHRONIZED;
                    return;
                }
            }
            state = State.SYNCHRONIZED;
        } finally {
	        operationsRepository.unlockWrite();
        }
    }

    public static void setHistoryCleaning(boolean historyCleaning) {
	    DistributedAuthApplication.historyCleaning = historyCleaning;
    }

    public static String getInstanceName() {
        return instanceName;
    }

    public static State getState() {
        return state;
    }

    public static void setState(State state) {
        DistributedAuthApplication.state = state;
    }

    public static long getLastConflictResolution() {
        return lastConflictResolution;
    }

    public static void setLastConflictResolution(long lastConflictResolution) {
        DistributedAuthApplication.lastConflictResolution = lastConflictResolution;
    }
}
