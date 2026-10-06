package com.thonha.backend.controller;

import com.thonha.backend.dto.review.CreateReviewRequest;
import com.thonha.backend.dto.review.ReviewResponse;
import com.thonha.backend.dto.review.WorkerReviewsResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {
    private final ReviewService service;
    private final CurrentUserProvider current;

    public ReviewController(ReviewService service, CurrentUserProvider current) {
        this.service = service;
        this.current = current;
    }

    /** Khách đánh giá yêu cầu của chính mình (khách lấy từ JWT, không nhận từ client). */
    @PostMapping("/repair-requests/{id}/review")
    ResponseEntity<ReviewResponse> create(@PathVariable Long id, @Valid @RequestBody CreateReviewRequest body) {
        return ResponseEntity.status(201).body(service.create(current.requireUserId(), id, body));
    }

    /** 200 + đánh giá, hoặc 204 nếu yêu cầu chưa được đánh giá. */
    @GetMapping("/repair-requests/{id}/review")
    ResponseEntity<ReviewResponse> get(@PathVariable Long id) {
        return service.getForRequest(id, current.requireUserId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Thợ xem đánh giá của chính mình. */
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/worker/reviews")
    WorkerReviewsResponse myReviews(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return service.listForWorker(current.requireUserId(), page, size);
    }
}