package com.thonha.backend.controller.admin;

import com.thonha.backend.common.NotFoundException;
import com.thonha.backend.dto.response.UserResponse;
import com.thonha.backend.enums.UserStatus;
import com.thonha.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {
    private final UserRepository repo;

    public UserAdminController(UserRepository r) {
        repo = r;
    }

    @GetMapping
    @Transactional(readOnly = true)
    Page<UserResponse> list(Pageable p) {
        return repo.findAll(p).map(UserResponse::from);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    UserResponse get(@PathVariable Long id) {
        return UserResponse.from(repo.findById(id).orElseThrow(() -> new NotFoundException("User not found")));
    }

    // open-in-view=false: phải có @Transactional + save thì thay đổi trạng thái mới được lưu
    @PatchMapping("/{id}/status")
    @Transactional
    UserResponse status(@PathVariable Long id, @RequestParam UserStatus status) {
        var u = repo.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        u.setStatus(status);
        return UserResponse.from(repo.save(u));
    }
}
