package com.thonha.vuatho.controller.admin;

import com.thonha.vuatho.dto.UserResponse;
import com.thonha.vuatho.entity.UserStatus;
import com.thonha.vuatho.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {
    private final UserRepository repo;

    public UserAdminController(UserRepository r) {
        repo = r;
    }

    @GetMapping
    Page<UserResponse> list(Pageable p) {
        return repo.findAll(p).map(UserResponse::from);
    }

    @PatchMapping("/{id}/status")
    UserResponse status(@PathVariable Long id, @RequestParam UserStatus status) {
        var u = repo.findById(id).orElseThrow();
        u.setStatus(status);
        return UserResponse.from(u);
    }
}
