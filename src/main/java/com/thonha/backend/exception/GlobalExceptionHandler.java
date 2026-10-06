package com.thonha.backend.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<?> una(UnauthorizedException e) {
        return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler({BadRequestException.class, IllegalStateException.class})
    ResponseEntity<?> bad(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<?> nf(NotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> val(MethodArgumentNotValidException e) {
        var m = new LinkedHashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(x -> m.put(x.getField(), x.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", m));
    }
}
