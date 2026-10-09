package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.RejectWorkerRequest;
import com.thonha.backend.dto.request.WorkerProfileSearchRequest;
import com.thonha.backend.dto.admin.WorkerApprovalResponse;
import com.thonha.backend.entity.*;
import com.thonha.backend.enums.ApprovalStatus;
import com.thonha.backend.enums.UserStatus;
import com.thonha.backend.repository.RoleRepository;
import com.thonha.backend.repository.UserRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thonha.backend.enums.DocumentType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkerApprovalService {
    private final WorkerProfileRepository workerProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public WorkerApprovalService(WorkerProfileRepository workerProfileRepository, UserRepository userRepository,
                                 RoleRepository roleRepository) {
        this.workerProfileRepository = workerProfileRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public Page<WorkerApprovalResponse> search(WorkerProfileSearchRequest request, Pageable pageable) {
        return workerProfileRepository.search(request.getStatus(), blank(request.getKeyword()), blank(request.getCity()), pageable)
                .map(WorkerApprovalResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkerApprovalResponse.Stats stats() {
        return new WorkerApprovalResponse.Stats(
                workerProfileRepository.countByApprovalStatus(ApprovalStatus.PENDING),
                workerProfileRepository.countByApprovalStatus(ApprovalStatus.APPROVED),
                workerProfileRepository.countByApprovalStatus(ApprovalStatus.REJECTED));
    }

    @Transactional(readOnly = true)
    public WorkerApprovalResponse getDetail(Long id) {
        return workerProfileRepository.findWithDetailById(id)
                .map(WorkerApprovalResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
    }

    @Transactional
    public WorkerApprovalResponse approve(Long profileId, Long adminId) {
        User admin = getAdmin(adminId);
        WorkerProfile profile = workerProfileRepository.findWithDetailById(profileId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));

        if (profile.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Hồ sơ không ở trạng thái chờ duyệt");
        }

        if (profile.getUser().getStatus() == UserStatus.LOCKED) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản thợ đang bị khóa, không thể duyệt hồ sơ");
        }

        List<DocumentType> missing = WorkerApprovalResponse.findMissingDocuments(profile);
        if (!missing.isEmpty()) {
            String names = missing.stream().map(DocumentType::getDescription).collect(Collectors.joining(", "));
            throw new ApiException(ErrorCode.MISSING_REQUIRED_DOCUMENTS, "Hồ sơ còn thiếu giấy tờ bắt buộc: " + names);
        }

        // Đảm bảo tài khoản có role WORKER khi được duyệt (idempotent, lúc đăng ký có thể đã được cấp)
        User workerUser = profile.getUser();
        if (!workerUser.hasRole(Role.WORKER)) {
            workerUser.getRoles().add(roleRepository.findByName(Role.WORKER)
                    .orElseThrow(() -> new IllegalStateException("Role not configured: " + Role.WORKER)));
        }

        profile.setApprovalStatus(ApprovalStatus.APPROVED);
        profile.setRejectReason(null);
        profile.setReviewedBy(admin);
        profile.setReviewedAt(LocalDateTime.now());

        return WorkerApprovalResponse.from(workerProfileRepository.save(profile));
    }

    @Transactional
    public WorkerApprovalResponse reject(Long profileId, Long adminId, RejectWorkerRequest request) {
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

        return WorkerApprovalResponse.from(workerProfileRepository.save(profile));
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