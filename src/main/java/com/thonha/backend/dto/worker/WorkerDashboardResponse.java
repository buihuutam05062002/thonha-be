package com.thonha.backend.dto.worker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Dữ liệu tổng hợp cho trang dashboard của thợ. */
public record WorkerDashboardResponse(
        Summary summary,
        List<IncomingRequest> incomingRequests,
        List<RecentOrder> recentOrders,
        List<DailyIncome> weeklyIncome) {

    public record Summary(
            int completedToday,
            BigDecimal incomeToday,
            BigDecimal incomeWeek,
            BigDecimal averageRating,
            long reviewCount,
            BigDecimal acceptanceRate,
            int ongoingJobs,
            int pendingRequests) {
    }

    /** Yêu cầu hệ thống vừa gửi cho thợ, đang chờ chấp nhận hoặc từ chối. */
    public record IncomingRequest(
            Long matchingLogId,
            Long requestId,
            String requestCode,
            String categoryName,
            String description,
            String priority,
            String priorityLabel,
            String addressText,
            String customerName,
            LocalDateTime sentAt,
            LocalDateTime expiresAt) {
    }

    public record RecentOrder(
            Long id,
            String requestCode,
            String customerName,
            String categoryName,
            String addressText,
            String status,
            String statusLabel,
            BigDecimal price,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record DailyIncome(LocalDate date, BigDecimal amount, int orders) {
    }
}
