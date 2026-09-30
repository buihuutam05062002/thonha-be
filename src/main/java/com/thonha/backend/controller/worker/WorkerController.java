package com.thonha.backend.controller.worker;

import com.thonha.backend.dto.worker.RegisterWorkerProfileRequest;
import com.thonha.backend.dto.worker.WorkerProfileResponse;
import com.thonha.backend.entity.WorkerProfile;
import com.thonha.backend.service.worker.WorkerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/worker")
@RequiredArgsConstructor
public class WorkerController {
    private final WorkerProfileService workerProfileService;
    
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkerProfileResponse> register(@RequestHeader("X-User-Id") Long userId,
                                                          @Valid @RequestPart("data") RegisterWorkerProfileRequest request,
                                                          @RequestPart(value = "cccdFront",required = false)MultipartFile cccdFront,
                                                          @RequestPart(value = "cccdBack",required = false)MultipartFile cccdBack,
                                                          @RequestPart(value = "certificates",required = false) List<MultipartFile> certificates,
                                                          @RequestPart(value = "degrees",required = false)List<MultipartFile> degrees){
        WorkerProfileResponse body = workerProfileService.register(userId, request, cccdFront, cccdBack, certificates, degrees);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
    
    @GetMapping("/profile")
    public WorkerProfileResponse getMyProfile(
            @RequestHeader("X-User-Id") Long userId) {         
        return workerProfileService.getMyProfile(userId);
    }
}
