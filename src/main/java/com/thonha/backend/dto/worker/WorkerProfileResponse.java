package com.thonha.backend.dto.worker;

import com.thonha.backend.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Comparator;

public record WorkerProfileResponse(Long id, String provinceCity, String operatingArea, Integer experienceYears,
                                    ApprovalStatus approvalStatus, AvailabilityStatus availabilityStatus,
                                    BigDecimal averageRating, BigDecimal acceptanceRate,
                                    Integer ongoingJobsCount, List<Specialty> specialties, List<Document> documents,
                                    Long reviewedById, LocalDateTime reviewedAt, String rejectReason) {
    public record Specialty(Long id, String name) {
    }

    public record Document(Long id, DocumentType type, String fileUrl) {
    }

    public static WorkerProfileResponse from(WorkerProfile p) {
        return new WorkerProfileResponse(p.getId(), p.getProvinceCity(), p.getOperatingArea(), p.getExperienceYears(), p.getApprovalStatus(), p.getAvailabilityStatus(), p.getAverageRating(), p.getAcceptanceRate(), p.getOngoingJobsCount(), p.getSpecialties().stream().map(s -> new Specialty(s.getServiceCategory().getId(), s.getServiceCategory().getName())).sorted(Comparator.comparing(Specialty::id)).toList(), p.getDocuments().stream().map(d -> new Document(d.getId(), d.getType(), d.getFileUrl())).toList(), p.getReviewedBy() == null ? null : p.getReviewedBy().getId(), p.getReviewedAt(), p.getRejectReason());
    }
}
