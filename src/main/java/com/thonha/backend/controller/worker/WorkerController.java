package com.thonha.backend.controller.worker;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.AvailabilityRequest;
import com.thonha.backend.dto.request.RegisterWorkerProfileRequest;
import com.thonha.backend.dto.response.WorkerProfileResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.WorkerService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/worker")
@PreAuthorize("hasAnyRole('WORKER','CUSTOMER')")
public class WorkerController {

    private final WorkerService workerService;
    private final CurrentUserProvider currentUserProvider;

    public WorkerController(WorkerService workerService, CurrentUserProvider currentUserProvider) {
        this.workerService = workerService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WorkerProfileResponse>> register(
            @Valid @RequestPart("data") RegisterWorkerProfileRequest data,
            @RequestPart("cccdFront") MultipartFile cccdFront,
            @RequestPart("cccdBack") MultipartFile cccdBack,
            @RequestPart(value = "certificates", required = false) List<MultipartFile> certificates,
            @RequestPart(value = "degrees", required = false) List<MultipartFile> degrees) {
        Long userId = currentUserProvider.requireUserId();
        WorkerProfileResponse response = workerService.register(userId, data, cccdFront, cccdBack, certificates, degrees);
        return ResponseEntity.status(201).body(ApiResponse.success(response, "Đăng ký hồ sơ thợ thành công"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<WorkerProfileResponse>> getProfile() {
        Long userId = currentUserProvider.requireUserId();
        WorkerProfileResponse response = workerService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('WORKER')")
    @PatchMapping("/availability")
    public ResponseEntity<ApiResponse<WorkerProfileResponse>> updateAvailability(
            @Valid @RequestBody AvailabilityRequest request) {
        Long userId = currentUserProvider.requireUserId();
        WorkerProfileResponse response = workerService.updateAvailability(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật trạng thái sẵn sàng thành công"));
    }
}