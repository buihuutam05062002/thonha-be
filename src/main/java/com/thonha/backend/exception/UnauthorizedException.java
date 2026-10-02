package com.thonha.backend.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String m) {
        super(m);
    }
}
