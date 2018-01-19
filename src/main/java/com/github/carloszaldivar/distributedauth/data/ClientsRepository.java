package com.github.carloszaldivar.distributedauth.data;

import com.github.carloszaldivar.distributedauth.models.Client;
import com.google.common.collect.ImmutableMap;

public interface ClientsRepository {
    public ImmutableMap<String, Client> getAll();
    public Client get(String clientNumber);
    public void add(Client client);
    public void update(Client client);
    public void delete(String clientNumber);
    public boolean authenticateClient(String clientNumber, String pin);
    public boolean authorizeOperation(String clientNumber, String pin, String oneTimePassword);
    public boolean activateNewPasswordList(String clientNumber, String pin, String oneTimePassword);
    public void clear();
}