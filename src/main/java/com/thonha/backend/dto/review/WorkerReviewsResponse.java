package com.thonha.backend.dto.review;

import java.util.List;

public record WorkerReviewsResponse(double average, long count, int page, int totalPages, List<ReviewResponse> items) {
}