package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.worker.WorkerDashboardResponse;
import com.thonha.backend.dto.worker.WorkerDashboardResponse.*;
import com.thonha.backend.entity.MatchingLog;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.entity.WorkerProfile;
import com.thonha.backend.enums.ApprovalStatus;
import com.thonha.backend.enums.MatchingResult;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.repository.MatchingLogRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.ReviewRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkerDashboardService {

    private static final int RECENT_ORDERS = 5;
    private static final int WEEK_DAYS = 7;

    private final WorkerProfileRepository workerProfileRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final MatchingLogRepository matchingLogRepository;
    private final ReviewRepository reviewRepository;

    public WorkerDashboardService(WorkerProfileRepository workerProfileRepository,
                                  RepairRequestRepository repairRequestRepository,
                                  MatchingLogRepository matchingLogRepository,
                                  ReviewRepository reviewRepository) {
        this.workerProfileRepository = workerProfileRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.matchingLogRepository = matchingLogRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public WorkerDashboardResponse getDashboard(Long userId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
        if (profile.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ApiException(ErrorCode.WORKER_NOT_APPROVED);
        }
        Long workerId = profile.getId();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // ----- thu nhập 7 ngày gần nhất (tính theo thời điểm hoàn thành) -----
        LocalDate firstDay = today.minusDays(WEEK_DAYS - 1);
        List<RepairRequest> completed = repairRequestRepository.findByWorkerAndStatusSince(
                workerId, RepairStatus.COMPLETED, firstDay.atStartOfDay());

        List<DailyIncome> weekly = new ArrayList<>();
        BigDecimal incomeWeek = BigDecimal.ZERO;
        BigDecimal incomeToday = BigDecimal.ZERO;
        int completedToday = 0;
        for (int i = 0; i < WEEK_DAYS; i++) {
            LocalDate day = firstDay.plusDays(i);
            List<RepairRequest> ofDay = completed.stream()
                    .filter(r -> r.getUpdatedAt() != null && r.getUpdatedAt().toLocalDate().equals(day))
                    .toList();
            BigDecimal amount = ofDay.stream()
                    .map(r -> r.getFinalPrice() == null ? BigDecimal.ZERO : r.getFinalPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            weekly.add(new DailyIncome(day, amount, ofDay.size()));
            incomeWeek = incomeWeek.add(amount);
            if (day.equals(today)) {
                incomeToday = amount;
                completedToday = ofDay.size();
            }
        }

        // ----- yêu cầu đang chờ phản hồi -----
        LocalDateTime since = now.minusMinutes(MatchingService.MATCHING_TIMEOUT_MINUTES);
        List<IncomingRequest> incoming = matchingLogRepository
                .findPendingForWorker(workerId, MatchingResult.PENDING, since).stream()
                .map(this::toIncoming)
                .toList();

        // ----- đơn gần đây -----
        List<RecentOrder> recent = repairRequestRepository
                .findRecentByWorker(workerId, PageRequest.of(0, RECENT_ORDERS)).stream()
                .map(this::toRecent)
                .toList();

        Summary summary = new Summary(
                completedToday, incomeToday, incomeWeek,
                profile.getAverageRating(), reviewRepository.countByWorkerId(workerId),
                profile.getAcceptanceRate(), profile.getOngoingJobsCount(), incoming.size());

        return new WorkerDashboardResponse(summary, incoming, recent, weekly);
    }

    private IncomingRequest toIncoming(MatchingLog m) {
        RepairRequest r = m.getRequest();
        return new IncomingRequest(
                m.getId(), r.getId(), r.getRequestCode(),
                r.getCategory().getName(), r.getDescription(),
                r.getPriorityLevel().name(), r.getPriorityLevel().getDescription(),
                r.getAddressText(), r.getCustomer().getFullName(),
                m.getSentAt(), m.getSentAt().plusMinutes(MatchingService.MATCHING_TIMEOUT_MINUTES));
    }

    private RecentOrder toRecent(RepairRequest r) {
        return new RecentOrder(
                r.getId(), r.getRequestCode(), r.getCustomer().getFullName(),
                r.getCategory().getName(), r.getAddressText(),
                r.getStatus().name(), r.getStatus().getDescription(),
                r.getFinalPrice(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
