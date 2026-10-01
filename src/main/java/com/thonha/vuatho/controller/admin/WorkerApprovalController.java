package com.thonha.vuatho.controller.admin;

import com.thonha.vuatho.dto.admin.*;
import com.thonha.vuatho.dto.worker.WorkerProfileResponse;
import com.thonha.vuatho.entity.ApprovalStatus;
import com.thonha.vuatho.security.CurrentUserProvider;
import com.thonha.vuatho.service.WorkerApprovalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/worker-profiles")
public class WorkerApprovalController {
    private final WorkerApprovalService s;
    private final CurrentUserProvider c;

    public WorkerApprovalController(WorkerApprovalService s, CurrentUserProvider c) {
        this.s = s;
        this.c = c;
    }

    @GetMapping
    Page<WorkerProfileResponse> list(@RequestParam(required = false) ApprovalStatus status, @RequestParam(required = false) String keyword, @RequestParam(required = false) String city, Pageable pageable) {
        return s.search(status, keyword, city, pageable);
    }

    @GetMapping("/{id}")
    WorkerProfileResponse detail(@PathVariable Long id) {
        return s.detail(id);
    }

    @PatchMapping("/{id}/approve")
    WorkerProfileResponse approve(@PathVariable Long id) {
        return s.approve(id, c.requireUserId());
    }

    @PatchMapping("/{id}/reject")
    WorkerProfileResponse reject(@PathVariable Long id, @Valid @RequestBody RejectWorkerRequest body) {
        return s.reject(id, c.requireUserId(), body);
    }
}
