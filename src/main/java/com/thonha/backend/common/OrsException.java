package com.thonha.backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OrsException extends RuntimeException {

    private final HttpStatus status;

    public OrsException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public OrsException(HttpStatus status, String message) {
        this(status, message, null);
    }
}
