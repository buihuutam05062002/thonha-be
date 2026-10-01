package com.thonha.backend.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminWorkerProfileResponse {
    Long id;
    Long userId;
    String name;
    String email;
    String phoneNumber;
    String avatar;
    String serviceArea;
    String residenceCity;
    Integer yearsOfExperience;
    String approvalStatus;
    String approvalStatusText;
    BigDecimal avgRating;
    Long reviewedById;
    String reviewedByName;
    LocalDateTime reviewedAt;
    String rejectReason;
    List<DocumentResponse> documents;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DocumentResponse {
        Long id;
        String type;
        String url;
    }
}