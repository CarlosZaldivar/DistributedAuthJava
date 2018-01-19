package com.github.carloszaldivar.distributedauth.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Neighbour {
    @JsonProperty("Id")
    final private String id;
    @JsonProperty("Url")
    final private String url;
    @JsonProperty("IsSpecial")
    final private boolean isSpecial;

    @JsonCreator
    public Neighbour(@JsonProperty("Id") String id, @JsonProperty("Url") String url) {
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
