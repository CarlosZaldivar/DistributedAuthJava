package com.github.carloszaldivar.distributedauth.data;

import com.github.carloszaldivar.distributedauth.models.Neighbour;

import java.util.*;

public class Neighbours {
    private static Map<String, Long> syncTimes = new HashMap<>();
    private static List<Neighbour> neighbours = new ArrayList<>();

    private Neighbours() {}

    public static Map<String, Long> getSyncTimes() {
        return syncTimes;
    }

    public static List<Neighbour> get() {
        return neighbours;
    }
}
