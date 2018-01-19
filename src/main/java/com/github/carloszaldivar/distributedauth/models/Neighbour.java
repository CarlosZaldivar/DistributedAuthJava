package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Neighbour {
    final private String id;
    final private String url;
    final private boolean isSpecial;

    @JsonCreator
    public Neighbour(@JsonProperty("id") String id, @JsonProperty("url") String url) {
        this.id = id;
        this.url = url;
        this.isSpecial = false;
    }

    public Neighbour(String id, String url, boolean isSpecial) {
        this.id = id;
        this.url = url;
        this.isSpecial = isSpecial;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public boolean isSpecial() {
        return isSpecial;
    }
}
