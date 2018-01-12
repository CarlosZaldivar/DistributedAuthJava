package com.github.carloszaldivar.distributedauth.data;

import com.github.carloszaldivar.distributedauth.models.Client;

import java.util.HashMap;
import java.util.Map;

public class Clients {
    private static Map<String, Client> clients = new HashMap<>();

    private Clients() {}

    public static Map<String, Client> get() {
        return clients;
    }
}
