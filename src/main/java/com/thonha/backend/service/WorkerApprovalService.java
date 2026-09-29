package com.thonha.backend.service;

import com.thonha.backend.dto.request.WorkerProfileSearchRequest;
import com.thonha.backend.dto.response.WorkerProfileResponse;
import com.thonha.backend.dto.response.WorkerProfileResponse.DocumentResponse;
import com.thonha.backend.entity.*;
import com.thonha.backend.repository.UserRepository;
import com.thonha.backend.repository.WalletRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkerApprovalService {

    // Role được phép duyệt. Thêm role mới (vd. "STAFF") chỉ cần sửa ở đây.
    static Set<String> REVIEWER_ROLES = Set.of("ADMIN", "STAFF");

    WorkerProfileRepository workerProfileRepository;
    UserRepository userRepository;
    WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public Page<WorkerProfileResponse> search(WorkerProfileSearchRequest req, Pageable pageable) {
        return workerProfileRepository
                .search(req.getStatus(), blankToNull(req.getKeyword()), blankToNull(req.getCity()), pageable)
                .map(w -> toResponse(w, false));
    }

    @Transactional(readOnly = true)
    public WorkerProfileResponse getDetail(Long id) {
        WorkerProfile w = workerProfileRepository.findWithDetailById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ thợ: " + id));
        return toResponse(w, true);
    }

    @Transactional
    public WorkerProfileResponse approve(Long profileId, Long reviewerId) {
        Users reviewer = loadReviewer(reviewerId);
        WorkerProfile w = loadPendingForUpdate(profileId);

        if (w.getUser().getUserStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Tài khoản của thợ không ở trạng thái hoạt động");
        }
        boolean hasIdCard = w.getDocuments().stream().anyMatch(d -> d.getType() == DocumentType.ID_CARD);
        if (!hasIdCard) {
            throw new IllegalStateException("Hồ sơ chưa có giấy tờ tùy thân (CCCD)");
        }

        w.setApprovalStatus(ApprovalStatus.APPROVED);
        w.setRejectReason(null);
        stamp(w, reviewer);

        if (!walletRepository.existsByWorkerId(w.getId())) {
            Wallet wallet = new Wallet();
            wallet.setWorker(w);
            walletRepository.save(wallet);
        }
        return toResponse(w, true);
    }

    @Transactional
    public WorkerProfileResponse reject(Long profileId, Long reviewerId, String reason) {
        Users reviewer = loadReviewer(reviewerId);
        WorkerProfile w = loadPendingForUpdate(profileId);

        w.setApprovalStatus(ApprovalStatus.REJECTED);
        w.setRejectReason(reason.trim());
        stamp(w, reviewer);
        return toResponse(w, true);
    }

    // ---------- helpers ----------

    private WorkerProfile loadPendingForUpdate(Long id) {
        WorkerProfile w = workerProfileRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ thợ: " + id));
        if (w.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException(
                    "Hồ sơ đã được xử lý (" + w.getApprovalStatus().getDescription() + ")");
        }
        return w;
    }

    private void stamp(WorkerProfile w, Users reviewer) {
        w.setReviewedBy(reviewer);
        w.setReviewedAt(LocalDateTime.now());
    }

    /** Điểm kiểm tra quyền duy nhất. Sau này thay bằng Spring Security. */
    private Users loadReviewer(Long reviewerId) {
        Users u = userRepository.findById(reviewerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người duyệt: " + reviewerId));
        if (u.getUserStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Tài khoản người duyệt không hoạt động");
        }
        boolean allowed = u.getRoles().stream().anyMatch(r -> REVIEWER_ROLES.contains(r.getName()));
        if (!allowed) {
            throw new IllegalStateException("Bạn không có quyền duyệt hồ sơ thợ");
        }
        return u;
    }

    private WorkerProfileResponse toResponse(WorkerProfile w, boolean withDocs) {
        Users u = w.getUser();
        Users r = w.getReviewedBy();
        List<DocumentResponse> docs = withDocs
                ? w.getDocuments().stream()
                .map(d -> new DocumentResponse(d.getId(), d.getType().name(), d.getUrl()))
                .toList()
                : List.of();

        return WorkerProfileResponse.builder()
                .id(w.getId())
                .userId(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .avatar(u.getAvatar())
                .serviceArea(w.getServiceArea())
                .residenceCity(w.getResidenceCity())
                .yearsOfExperience(w.getYearsOfExperience())
                .approvalStatus(w.getApprovalStatus().name())
                .approvalStatusText(w.getApprovalStatus().getDescription())
                .avgRating(w.getAvgRating())
                .reviewedById(r != null ? r.getId() : null)
                .reviewedByName(r != null ? r.getName() : null)
                .reviewedAt(w.getReviewedAt())
                .rejectReason(w.getRejectReason())
                .documents(docs)
                .build();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}