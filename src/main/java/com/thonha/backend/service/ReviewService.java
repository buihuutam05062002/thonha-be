package com.thonha.backend.service;

import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.dto.review.CreateReviewRequest;
import com.thonha.backend.dto.review.ReviewResponse;
import com.thonha.backend.dto.review.WorkerReviewsResponse;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.entity.Review;
import com.thonha.backend.entity.WorkerProfile;
import com.thonha.backend.common.BadRequestException;
import com.thonha.backend.common.NotFoundException;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.ReviewRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** Đánh giá sau khi hoàn thành: khách tạo, khách + thợ của yêu cầu xem, thợ xem toàn bộ đánh giá của mình. */
@Service
public class ReviewService {
    private static final String ALREADY = "Bạn đã đánh giá yêu cầu này rồi";

    private final ReviewRepository reviews;
    private final RepairRequestRepository requests;
    private final WorkerProfileRepository workers;

    public ReviewService(ReviewRepository reviews, RepairRequestRepository requests, WorkerProfileRepository workers) {
        this.reviews = reviews;
        this.requests = requests;
        this.workers = workers;
    }

    @Transactional
    public ReviewResponse create(Long customerId, Long requestId, CreateReviewRequest dto) {
        // Cùng một lỗi 404 cho "không tồn tại" và "của người khác" để không lộ id.
        RepairRequest r = requests.findByIdAndCustomerId(requestId, customerId)
                .orElseThrow(() -> new NotFoundException("Repair request not found"));
        if (r.getStatus() != RepairStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể đánh giá khi yêu cầu đã hoàn thành");
        }
        if (r.getWorker() == null) {
            throw new BadRequestException("Yêu cầu này chưa có thợ để đánh giá");
        }
        if (reviews.existsByRequestId(requestId)) {
            throw new BadRequestException(ALREADY);
        }

        Review rv = new Review();
        rv.setRequest(r);
        rv.setCustomer(r.getCustomer());
        rv.setWorker(r.getWorker());
        rv.setRating(dto.rating());
        String comment = dto.comment() == null ? null : dto.comment().trim();
        rv.setComment(comment == null || comment.isEmpty() ? null : comment);
        try {
            reviews.saveAndFlush(rv);
        } catch (DataIntegrityViolationException e) {
            // Hai lần bấm gửi cùng lúc: ràng buộc UNIQUE(request_id) ở DB chặn lần thứ hai.
            throw new BadRequestException(ALREADY);
        }
        refreshWorkerRating(r.getWorker().getId());
        return ReviewResponse.from(rv);
    }

    /** Trả về đánh giá của yêu cầu, hoặc empty nếu chưa có. Chỉ khách chủ yêu cầu hoặc thợ được ghép mới xem được. */
    @Transactional(readOnly = true)
    public Optional<ReviewResponse> getForRequest(Long requestId, Long userId) {
        boolean allowed = requests.existsByIdAndCustomerId(requestId, userId)
                || requests.existsByIdAndWorker_User_Id(requestId, userId);
        if (!allowed) {
            throw new NotFoundException("Repair request not found");
        }
        return reviews.findByRequestId(requestId).map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkerReviewsResponse listForWorker(Long workerUserId, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        Page<Review> p = reviews.findByWorker_User_IdOrderByCreatedAtDesc(workerUserId,
                PageRequest.of(Math.max(0, page), safeSize));
        ReviewRepository.RatingStats st = reviews.statsForWorkerUser(workerUserId);
        double avg = st.getAvg() == null ? 0 : BigDecimal.valueOf(st.getAvg()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        return new WorkerReviewsResponse(avg, st.getCnt() == null ? 0 : st.getCnt(), p.getNumber(), p.getTotalPages(),
                p.getContent().stream().map(ReviewResponse::from).toList());
    }

    /** Tính lại điểm trung bình từ toàn bộ đánh giá (tự sửa sai nếu hai đánh giá được lưu gần như cùng lúc). */
    private void refreshWorkerRating(Long workerId) {
        ReviewRepository.RatingStats st = reviews.statsForWorker(workerId);
        WorkerProfile w = workers.findById(workerId).orElseThrow(() -> new NotFoundException("Worker not found"));
        double avg = st.getAvg() == null ? 0 : st.getAvg();
        w.setAverageRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
    }
}