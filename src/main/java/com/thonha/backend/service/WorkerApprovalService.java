package com.thonha.backend.service;

import com.thonha.backend.dto.admin.RejectWorkerRequest;
import com.thonha.backend.dto.worker.WorkerProfileResponse;
import com.thonha.backend.entity.*;
import com.thonha.backend.exception.*;
import com.thonha.backend.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class WorkerApprovalService {
    private final WorkerProfileRepository profiles;
    private final UserRepository users;

    public WorkerApprovalService(WorkerProfileRepository p, UserRepository u) {
        profiles = p;
        users = u;
    }

    @Transactional(readOnly = true)
    public Page<WorkerProfileResponse> search(ApprovalStatus status, String keyword, String city, Pageable pageable) {
        return profiles.search(status, blank(keyword), blank(city), pageable).map(WorkerProfileResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkerProfileResponse detail(Long id) {
        return WorkerProfileResponse.from(profiles.findWithDetailById(id).orElseThrow(() -> new NotFoundException("Worker profile not found")));
    }

    @Transactional
    public WorkerProfileResponse approve(Long profileId, Long adminId) {
        User admin = admin(adminId);
        WorkerProfile p = profiles.findWithDetailById(profileId).orElseThrow(() -> new NotFoundException("Worker profile not found"));
        if (p.getApprovalStatus() != ApprovalStatus.PENDING)
            throw new BadRequestException("Worker profile is not pending");
        boolean front = p.getDocuments().stream().anyMatch(d -> d.getType() == DocumentType.CCCD_FRONT), back = p.getDocuments().stream().anyMatch(d -> d.getType() == DocumentType.CCCD_BACK);
        if (!front || !back) throw new BadRequestException("Worker profile must contain both CCCD images");
        p.setApprovalStatus(ApprovalStatus.APPROVED);
        p.setRejectReason(null);
        p.setReviewedBy(admin);
        p.setReviewedAt(LocalDateTime.now());
        return WorkerProfileResponse.from(p);
    }

    @Transactional
    public WorkerProfileResponse reject(Long profileId, Long adminId, RejectWorkerRequest body) {
        User admin = admin(adminId);
        WorkerProfile p = profiles.findWithDetailById(profileId).orElseThrow(() -> new NotFoundException("Worker profile not found"));
        if (p.getApprovalStatus() != ApprovalStatus.PENDING)
            throw new BadRequestException("Worker profile is not pending");
        p.setApprovalStatus(ApprovalStatus.REJECTED);
        p.setRejectReason(body.reason().trim());
        p.setReviewedBy(admin);
        p.setReviewedAt(LocalDateTime.now());
        return WorkerProfileResponse.from(p);
    }

    private User admin(Long id) {
        User u = users.findById(id).orElseThrow(() -> new UnauthorizedException("Admin not found"));
        if (!u.hasRole(Role.ADMIN)) throw new UnauthorizedException("Admin role required");
        return u;
    }

    private String blank(String x) {
        return x == null || x.isBlank() ? null : x.trim();
    }
}
