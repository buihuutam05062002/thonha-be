package com.thonha.backend.controller;

import com.thonha.backend.dto.UserResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final AuthService s;
    private final CurrentUserProvider current;

    public UserController(AuthService s, CurrentUserProvider c) {
        this.s = s;
        current = c;
    }

    @GetMapping("/me")
    UserResponse me() {
        return s.me(current.requireUserId());
    }

    @PutMapping("/me")
    UserResponse update(@RequestBody UpdateProfileRequest r) {
        return s.update(current.requireUserId(), r.fullName(), r.avatarUrl());
    }

    public record UpdateProfileRequest(@NotBlank String fullName, String avatarUrl) {
    }
}
