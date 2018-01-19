package com.github.carloszaldivar.distributedauth.data;

import com.github.carloszaldivar.distributedauth.models.Operation;
import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class LocalOperationsRepository implements OperationsRepository {
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static LinkedList<Operation> operations = new LinkedList<>();

    @Override
    public void lockRead() {
        lock.readLock().lock();
    }

    @Override
    public void unlockRead() {
        lock.readLock().unlock();
    }

    @Override
    public void lockWrite() {
        lock.writeLock().lock();
    }

    @Override
    public void unlockWrite() {
        lock.writeLock().unlock();
    }

    @Override
    public ImmutableList<Operation> getAll() {
        lock.readLock().lock();
        try {
            return ImmutableList.<Operation>builder().addAll(operations).build();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Operation getLast() {
        lock.readLock().lock();
        try {
            if (operations.isEmpty()) {
                return null;
            }
            return operations.get(operations.size() - 1);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addToEnd(Operation operation) {
        lock.writeLock().lock();
        try {
            operations.add(operation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addToEnd(List<Operation> newOperations) {
        lock.writeLock().lock();
        try {
            operations.addAll(newOperations);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeFromEnd(int numberToRemove) {
        lock.writeLock().lock();
        if (numberToRemove > operations.size()) {
            throw new IllegalArgumentException("Too many operations to remove");
        }
        try {
            while (numberToRemove > 0) {
                operations.removeLast();
                --numberToRemove;
            }
        } finally {
            lock.writeLock().unlock();
        }    }

    @Override
    public void removeFromStart(int numberToRemove) {
        lock.writeLock().lock();
        if (numberToRemove > operations.size()) {
            throw new IllegalArgumentException("Too many operations to remove");
        }
        try {
            while (numberToRemove > 0) {
                operations.removeFirst();
                --numberToRemove;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            operations.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
