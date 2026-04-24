package io.vanguard.testops.api.support.regex.exception;

public class UninitializedException extends Exception {

    public UninitializedException() {
        super();
    }

    public UninitializedException(String message) {
        super(message);
    }
}
