package com.thonha.backend.controller;

import com.thonha.backend.dto.request.UserSearchRequest;
import com.thonha.backend.dto.request.UserCreateRequest;
import com.thonha.backend.dto.response.UserResponse;
import com.thonha.backend.entity.UserStatus;
import com.thonha.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAll(
            UserSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(userService.getUsers(request, pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> setStatus(@PathVariable Long id, @RequestParam UserStatus userStatus) {
        return ResponseEntity.ok(userService.setStatus(id, userStatus));
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }
}
