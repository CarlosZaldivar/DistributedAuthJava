package com.github.carloszaldivar.distributedauth;

import java.util.HashMap;
import java.util.Map;

public class Clients {
    private static Map<String, Client> clients = new HashMap<>();

    private Clients() {}

    public static Map<String, Client> get() {
        return clients;
    }
}
