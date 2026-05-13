package com.daily.lastsys.features.signup;

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
