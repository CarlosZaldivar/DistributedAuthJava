package com.github.carloszaldivar.distributedauth.data;

import com.github.carloszaldivar.distributedauth.models.Operation;
import com.google.common.collect.ImmutableList;

import java.util.List;

public interface OperationsRepository {
    ImmutableList<Operation> getAll();
    Operation getLast();
    public void lockRead();
    public void unlockRead();
    public void lockWrite();
    public void unlockWrite();
    public void addToEnd(List<Operation> operation);
    public void addToEnd(Operation operation);
    public void removeFromEnd(int numberToRemove);
    public void removeFromStart(int numberToRemove);
    public void clear();
}
