package com.thonha.backend.controller.worker;

import com.thonha.backend.dto.worker.*;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.WorkerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/v1/worker")
@PreAuthorize("hasAnyRole('WORKER','CUSTOMER')")
public class WorkerController {
    private final WorkerService s;
    private final CurrentUserProvider c;

    public WorkerController(WorkerService s, CurrentUserProvider c) {
        this.s = s;
        this.c = c;
    }

    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<WorkerProfileResponse> register(@Valid @RequestPart("data") RegisterWorkerProfileRequest data, @RequestPart("cccdFront") MultipartFile front, @RequestPart("cccdBack") MultipartFile back, @RequestPart(value = "certificates", required = false) List<MultipartFile> certificates, @RequestPart(value = "degrees", required = false) List<MultipartFile> degrees) {
        return ResponseEntity.status(201).body(s.register(c.requireUserId(), data, front, back, certificates, degrees));
    }

//    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/profile")
    WorkerProfileResponse me() {
        return s.me(c.requireUserId());
    }

    @PreAuthorize("hasRole('WORKER')")
    @PatchMapping("/availability")
    WorkerProfileResponse availability(@Valid @RequestBody AvailabilityRequest r) {
        return s.updateAvailability(c.requireUserId(), r.available());
    }
}
