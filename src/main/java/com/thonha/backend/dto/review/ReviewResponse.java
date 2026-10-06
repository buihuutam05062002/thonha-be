package com.thonha.backend.dto.review;

import com.thonha.backend.entity.Review;

import java.time.LocalDateTime;

/** customerName chỉ hiện tên (không hiện họ) để thợ không thấy đầy đủ họ tên khách. */
public record ReviewResponse(Long id, Long requestId, String requestCode, int rating, String comment,
                             String customerName, LocalDateTime createdAt) {

    public static ReviewResponse from(Review r) {
        return new ReviewResponse(r.getId(), r.getRequest().getId(), r.getRequest().getRequestCode(),
                r.getStarRating(), r.getComment(), shortName(r.getCustomer().getFullName()), r.getCreatedAt());
    }

    private static String shortName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Khách hàng";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }
}