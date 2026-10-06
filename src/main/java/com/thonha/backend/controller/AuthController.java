package com.thonha.backend.controller;

import com.thonha.backend.dto.*;
import com.thonha.backend.dto.auth.*;
import com.thonha.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService s;

    public AuthController(AuthService s) {
        this.s = s;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest r
    ) {
        return ResponseEntity
                .status(201)
                .body(s.register(r));
    }

    @PostMapping("/login")
    AuthResponse login(
            @Valid @RequestBody LoginRequest r
    ) {
        return s.login(r);
    }

    @PostMapping("/refresh")
    AuthResponse refresh(
            @Valid @RequestBody RefreshRequest r
    ) {
        return s.refresh(r.refreshToken());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshRequest r
    ) {
        if (r != null) {
            s.logout(r.refreshToken());
        }

        return ResponseEntity.noContent().build();
    }
}