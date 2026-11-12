package io.github.screeningloop.identity;

public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException() {
        super("Too many failed attempts. Try again later");
    }
}
