package com.worklink.exception;

public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException() {
        super("A profile already exists for this account");
    }
}
