package com.github.carloszaldivar.distributedauth;

import java.util.ArrayList;
import java.util.List;

public class Operations {
    private static List<Operation> operations = new ArrayList<>();

    private Operations() {}

    public static List<Operation> get() {
        return operations;
    }
}
