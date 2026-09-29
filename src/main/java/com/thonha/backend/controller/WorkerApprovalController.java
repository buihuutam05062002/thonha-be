package com.thonha.backend.controller;

import com.thonha.backend.dto.request.RejectWorkerRequest;
import com.thonha.backend.dto.request.WorkerProfileSearchRequest;
import com.thonha.backend.dto.response.WorkerProfileResponse;
import com.thonha.backend.service.WorkerApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worker-profiles")
@RequiredArgsConstructor
public class WorkerApprovalController {

    private final WorkerApprovalService service;

    // GET /api/admin/worker-profiles?status=PENDING&keyword=an&page=0&size=10
    @GetMapping
    public Page<WorkerProfileResponse> list(
            WorkerProfileSearchRequest req,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(req, pageable);
    }

    @GetMapping("/{id}")
    public WorkerProfileResponse detail(@PathVariable Long id) {
        return service.getDetail(id);
    }

    @PatchMapping("/{id}/approve")
    public WorkerProfileResponse approve(@PathVariable Long id,
                                         @RequestHeader("X-Reviewer-Id") Long reviewerId) {
        return service.approve(id, reviewerId);
    }

    @PatchMapping("/{id}/reject")
    public WorkerProfileResponse reject(@PathVariable Long id,
                                        @RequestHeader("X-Reviewer-Id") Long reviewerId,
                                        @Valid @RequestBody RejectWorkerRequest body) {
        return service.reject(id, reviewerId, body.getReason());
    }
}