package com.company.server.Exceptions;

public class NonExistingConfigParam extends Exception {
    public NonExistingConfigParam(String message) {
        super(message);
    }
}
