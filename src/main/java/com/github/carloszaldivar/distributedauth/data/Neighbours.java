package com.github.carloszaldivar.distributedauth.data;

import com.github.carloszaldivar.distributedauth.models.Neighbour;

import java.util.*;

public class Neighbours {
    private static Map<String, Long> syncTimes = new HashMap<>();
    private static Map<String, Neighbour> neighbours = new HashMap<>();

    private Neighbours() {}

    public static Map<String, Long> getSyncTimes() {
        return syncTimes;
    }

    public static Map<String, Neighbour> get() {
        return neighbours;
    }
}
