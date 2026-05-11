package com.daily.lastsys.service;

public class SignupDuplicateException extends RuntimeException {

    private final String field;

    public SignupDuplicateException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
