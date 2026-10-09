package com.thonha.backend.dto.response;

import com.thonha.backend.enums.ApprovalStatus;
import com.thonha.backend.enums.AvailabilityStatus;
import com.thonha.backend.enums.DocumentType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerProfileResponse {
    private Long id;
    private String provinceCity;
    private String operatingArea;
    private Integer experienceYears;
    private ApprovalStatus approvalStatus;
    private AvailabilityStatus availabilityStatus;
    private BigDecimal averageRating;
    private BigDecimal acceptanceRate;
    private Integer ongoingJobsCount;
    private List<SpecialtyResponse> specialties;
    private List<DocumentResponse> documents;
    private Long reviewedById;
    private LocalDateTime reviewedAt;
    private String rejectReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialtyResponse {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentResponse {
        private Long id;
        private DocumentType type;
        private String fileUrl;
    }

    public static WorkerProfileResponse from(com.thonha.backend.entity.WorkerProfile p) {
        if (p == null) return null;
        List<SpecialtyResponse> specialties = p.getSpecialties() != null 
            ? p.getSpecialties().stream()
                .map(s -> SpecialtyResponse.builder()
                        .id(s.getServiceCategory().getId())
                        .name(s.getServiceCategory().getName())
                        .build())
                .sorted(Comparator.comparing(SpecialtyResponse::getId))
                .toList()
            : List.of();

        List<DocumentResponse> documents = p.getDocuments() != null
            ? p.getDocuments().stream()
                .map(d -> DocumentResponse.builder()
                        .id(d.getId())
                        .type(d.getType())
                        .fileUrl(d.getFileUrl())
                        .build())
                .toList()
            : List.of();

        return WorkerProfileResponse.builder()
                .id(p.getId())
                .provinceCity(p.getProvinceCity())
                .operatingArea(p.getOperatingArea())
                .experienceYears(p.getExperienceYears())
                .approvalStatus(p.getApprovalStatus())
                .availabilityStatus(p.getAvailabilityStatus())
                .averageRating(p.getAverageRating())
                .acceptanceRate(p.getAcceptanceRate())
                .ongoingJobsCount(p.getOngoingJobsCount())
                .specialties(specialties)
                .documents(documents)
                .reviewedById(p.getReviewedBy() != null ? p.getReviewedBy().getId() : null)
                .reviewedAt(p.getReviewedAt())
                .rejectReason(p.getRejectReason())
                .build();
    }
}