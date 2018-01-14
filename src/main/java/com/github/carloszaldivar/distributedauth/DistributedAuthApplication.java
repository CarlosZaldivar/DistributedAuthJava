package com.github.carloszaldivar.distributedauth;

import com.github.carloszaldivar.distributedauth.data.Neighbours;
import com.github.carloszaldivar.distributedauth.data.Operations;
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
	private static boolean historyCleaning = true;

	public enum State { SYNCHRONIZED, UNSYNCHRONIZED, CONFLICT, TOO_OLD }

	public static void main(String[] args) {
        if (args.length > 0) {
	        instanceName = args[0];
        } else {
	        instanceName = "test-" + System.currentTimeMillis();
        }
        SpringApplication.run(DistributedAuthApplication.class, args);
	}

    /**
     * Change state to SYNCHRONIZED if possible.
     */
	public static void updateState() {
	    if (isSynchronized()) {
	        state = State.SYNCHRONIZED;
	        if (historyCleaning && Neighbours.get().size() > 0) {
	            Operations.get().clear();
            }
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

    private static boolean isSynchronized() {
        DistributedAuthApplication.State state = DistributedAuthApplication.getState();
        if (state == DistributedAuthApplication.State.TOO_OLD || state == DistributedAuthApplication.State.CONFLICT) {
            return false;
        }

        List<Operation> operations = Operations.get();
        if (operations.isEmpty()) {
            return true;
        }

        long lastTimestamp = operations.get(operations.size() - 1).getTimestamp();
        for (Neighbour neighbour : Neighbours.get()) {
            if (Neighbours.getSyncTimes().get(neighbour.getId()) != lastTimestamp) {
                return false;
            }
        }
        return true;
    }
}
