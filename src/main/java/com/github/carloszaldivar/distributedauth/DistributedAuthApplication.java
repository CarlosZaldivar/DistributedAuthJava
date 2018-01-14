package com.github.carloszaldivar.distributedauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DistributedAuthApplication {
	private static String instanceName;
	private static State state = State.SYNCHRONIZED;

	public enum State { SYNCHRONIZED, UNSYNCHRONIZED, CONFLICT, TOO_OLD }

	public static void main(String[] args) {
        if (args.length > 0) {
	        instanceName = args[0];
        } else {
	        instanceName = "test-" + System.currentTimeMillis();
        }
        SpringApplication.run(DistributedAuthApplication.class, args);
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
}
