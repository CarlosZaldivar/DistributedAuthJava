package com.github.carloszaldivar.distributedauth.data;

import com.github.carloszaldivar.distributedauth.models.Neighbour;
import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class LocalNeighboursRepository implements NeighboursRepository {
    final private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    final private static Map<String, Neighbour> neighbours = new HashMap<>();
    final private static Map<String, Long> syncTimes = new HashMap<>();

    @Override
    public ImmutableMap<String, Neighbour> getNeighbours() {
        lock.readLock().lock();
        try {
            return ImmutableMap.<String, Neighbour>builder().putAll(neighbours).build();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(Neighbour neighbour) {
        validateNeighbour(neighbour);
        lock.writeLock().lock();
        try {
            neighbours.put(neighbour.getId(), neighbour);
            syncTimes.put(neighbour.getId(), 0L);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(String neighbourId) {
        lock.writeLock().lock();
        try {
            validateNeighbourId(neighbourId);
            neighbours.remove(neighbourId);
            syncTimes.remove(neighbourId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void setSpecial(String neighbourId, boolean value) {
        lock.writeLock().lock();
        try {
            validateNeighbourId(neighbourId);
            Neighbour neighbour = neighbours.get(neighbourId);
            neighbours.put(neighbourId, new Neighbour(neighbour.getId(), neighbour.getUrl(), value));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ImmutableMap<String, Long> getSyncTimes() {
        lock.readLock().lock();
        try {
            return ImmutableMap.<String, Long>builder().putAll(syncTimes).build();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void updateSyncTime(String neighbourId, long timestamp) {
        validateNeighbourId(neighbourId);
        lock.writeLock().lock();
        try {
            syncTimes.put(neighbourId, timestamp);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            neighbours.clear();
            syncTimes.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void validateNeighbour(Neighbour neighbour) {
        if (neighbour == null) {
            throw new IllegalArgumentException("Neighbour cannot be null.");
        }
        if (StringUtils.isEmpty(neighbour.getId())) {
            throw new IllegalArgumentException("Neighbour ID cannot be null or empty.");
        }
        if (StringUtils.isEmpty(neighbour.getUrl())) {
            throw new IllegalArgumentException("Neighbour URL cannot be null or empty.");
        }
    }

    private void validateNeighbourId(String neighbourId) {
        if (StringUtils.isEmpty(neighbourId)) {
            throw new IllegalArgumentException("Neighbour ID cannot be null or empty.");
        }
        if (!neighbours.containsKey(neighbourId)) {
            throw new IllegalArgumentException("No neighbour with ID " + neighbourId);
        }
    }
}
