package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.RejectWorkerRequest;
import com.thonha.backend.dto.request.WorkerProfileSearchRequest;
import com.thonha.backend.dto.response.WorkerProfileResponse;
import com.thonha.backend.entity.*;
import com.thonha.backend.enums.ApprovalStatus;
import com.thonha.backend.enums.DocumentType;
import com.thonha.backend.repository.UserRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class WorkerApprovalService {
    private final WorkerProfileRepository workerProfileRepository;
    private final UserRepository userRepository;

    public WorkerApprovalService(WorkerProfileRepository workerProfileRepository, UserRepository userRepository) {
        this.workerProfileRepository = workerProfileRepository;
        this.userRepository = userRepository;
    }

    public Page<WorkerProfileResponse> search(WorkerProfileSearchRequest request, Pageable pageable) {
        return workerProfileRepository.search(request.getStatus(), blank(request.getKeyword()), blank(request.getCity()), pageable)
                .map(WorkerProfileResponse::from);
    }

    public WorkerProfileResponse getDetail(Long id) {
        return workerProfileRepository.findWithDetailById(id)
                .map(WorkerProfileResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
    }

    @Transactional
    public WorkerProfileResponse approve(Long profileId, Long adminId) {
        User admin = getAdmin(adminId);
        WorkerProfile profile = workerProfileRepository.findWithDetailById(profileId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));

        if (profile.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Hồ sơ không ở trạng thái chờ duyệt");
        }

        boolean hasFront = profile.getDocuments().stream()
                .anyMatch(d -> d.getType() == DocumentType.CCCD_FRONT);
        boolean hasBack = profile.getDocuments().stream()
                .anyMatch(d -> d.getType() == DocumentType.CCCD_BACK);
        if (!hasFront || !hasBack) {
            throw new ApiException(ErrorCode.MISSING_REQUIRED_DOCUMENTS, "Hồ sơ phải chứa cả hai ảnh CCCD");
        }

        profile.setApprovalStatus(ApprovalStatus.APPROVED);
        profile.setRejectReason(null);
        profile.setReviewedBy(admin);
        profile.setReviewedAt(LocalDateTime.now());

        return WorkerProfileResponse.from(workerProfileRepository.save(profile));
    }

    @Transactional
    public WorkerProfileResponse reject(Long profileId, Long adminId, RejectWorkerRequest request) {
        User admin = getAdmin(adminId);
        WorkerProfile profile = workerProfileRepository.findWithDetailById(profileId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));

        if (profile.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Hồ sơ không ở trạng thái chờ duyệt");
        }

        profile.setApprovalStatus(ApprovalStatus.REJECTED);
        profile.setRejectReason(request.getReason().trim());
        profile.setReviewedBy(admin);
        profile.setReviewedAt(LocalDateTime.now());

        return WorkerProfileResponse.from(workerProfileRepository.save(profile));
    }

    private User getAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (!user.hasRole(Role.ADMIN)) {
            throw new ApiException(ErrorCode.ADMIN_REQUIRED);
        }
        return user;
    }

    private String blank(String x) {
        return x == null || x.isBlank() ? null : x.trim();
    }
}