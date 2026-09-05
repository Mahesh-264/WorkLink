package com.worklink.exception;

public class InvalidRegistrationRoleException extends RuntimeException {

    public InvalidRegistrationRoleException() {
        super("Only CLIENT and FREELANCER roles may self-register");
    }
}
