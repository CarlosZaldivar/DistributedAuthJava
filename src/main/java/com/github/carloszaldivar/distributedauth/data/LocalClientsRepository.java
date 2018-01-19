package com.github.carloszaldivar.distributedauth.data;

import com.github.carloszaldivar.distributedauth.models.Client;
import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class LocalClientsRepository  implements ClientsRepository {
    final private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    final private static Map<String, Client> clients = new HashMap<>();

    @Override
    public ImmutableMap<String, Client> getAll() {
        lock.readLock().lock();
        try {
            return ImmutableMap.<String, Client>builder().putAll(clients).build();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Client get(String clientNumber) {
        lock.readLock().lock();
        try {
            return clients.get(clientNumber);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(Client client) {
        validateClientAddition(client);
        lock.writeLock().lock();
        try {
            clients.put(client.getNumber(), client);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Client client) {
        validateClientUpdate(client);
        lock.writeLock().lock();
        try {
            clients.put(client.getNumber(), client);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(String clientNumber) {
        lock.writeLock().lock();
        try {
            validateClientNumber(clientNumber);
            clients.remove(clientNumber);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean authenticateClient(String clientNumber, String pin) {
        if (StringUtils.isEmpty(clientNumber) || StringUtils.isEmpty(pin)) {
            return false;
        }
        lock.readLock().lock();
        try {
            if (!clients.containsKey(clientNumber)) {
                return false;
            }
            return clients.get(clientNumber).getPin().equals(pin);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean authorizeOperation(String clientNumber, String pin, String oneTimePassword) {
        if (StringUtils.isEmpty(clientNumber) || StringUtils.isEmpty(pin) || StringUtils.isEmpty(oneTimePassword)) {
            return false;
        }
        lock.writeLock().lock();
        try {
            if (!clients.containsKey(clientNumber)) {
                return false;
            }
            Client client = clients.get(clientNumber);
            return client.getPin().equals(pin) && client.useOneTimePassword(oneTimePassword);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean activateNewPasswordList(String clientNumber, String pin, String oneTimePassword) {
        lock.writeLock().lock();
        try {
            if (!clients.containsKey(clientNumber)) {
                return false;
            }
            Client client = clients.get(clientNumber);
            return client.getPin().equals(pin) && client.activateNewOneTimePasswordList(oneTimePassword);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            clients.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }


    private void validateClientAddition(Client client) {
        validateClientData(client);
        if (clients.containsKey(client.getNumber())) {
            throw new IllegalArgumentException("Client with given number already exists.");
        }
    }

    private void validateClientUpdate(Client client) {
        validateClientData(client);
        if (!clients.containsKey(client.getNumber())) {
            throw new IllegalArgumentException("Client with given number doesn't exists.");
        }
    }

    private void validateClientData(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Client cannot be null.");
        }
        if (StringUtils.isEmpty(client.getNumber())) {
            throw new IllegalArgumentException("Client number cannot be null or empty.");
        }
        if (StringUtils.isEmpty(client.getPin())) {
            throw new IllegalArgumentException("Client PIN cannot be null or empty.");
        }
        if (client.getActivatedList() == null) {
            throw new IllegalArgumentException("Activated list cannot be null.");
        }
        if (client.getNonactivatedList() == null) {
            throw new IllegalArgumentException("Nonactivated list cannot be null.");
        }
    }

    private void validateClientNumber(String clientNumber) {
        if (StringUtils.isEmpty(clientNumber)) {
            throw new IllegalArgumentException("Client number cannot be null or empty.");
        }
        if (!clients.containsKey(clientNumber)) {
            throw new IllegalArgumentException("No client with number " + clientNumber);
        }
    }
}
