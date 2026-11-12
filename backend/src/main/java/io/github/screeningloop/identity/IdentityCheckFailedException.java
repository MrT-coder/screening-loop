package io.github.screeningloop.identity;

public class IdentityCheckFailedException extends RuntimeException {

    public IdentityCheckFailedException() {
        super("Identity could not be verified");
    }
}
