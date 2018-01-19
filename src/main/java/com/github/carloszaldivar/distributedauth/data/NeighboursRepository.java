package com.github.carloszaldivar.distributedauth.data;

import com.github.carloszaldivar.distributedauth.models.Neighbour;
import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Repository;

@Repository
public interface NeighboursRepository {
    public ImmutableMap<String, Neighbour> getNeighbours();
    public void add(Neighbour neighbour);
    public void setSpecial(String neighbourId, boolean value);
    public ImmutableMap<String, Long> getSyncTimes();
    public void updateSyncTime(String neighbourId, long timestamp);
    public void clear();
}
