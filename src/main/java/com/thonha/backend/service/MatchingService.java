package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.entity.*;
import com.thonha.backend.enums.*;
import com.thonha.backend.repository.MatchingLogRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final WorkerProfileRepository workerProfileRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final MatchingLogRepository matchingLogRepository;
    private final GoongService goongService;
    private final NotificationService notificationService;

    // Cấu hình trọng số cho thuật toán matching
    private static final BigDecimal WEIGHT_DISTANCE = new BigDecimal("0.40");      // 40% - khoảng cách
    private static final BigDecimal WEIGHT_RATING = new BigDecimal("0.25");        // 25% - rating
    private static final BigDecimal WEIGHT_ACCEPTANCE = new BigDecimal("0.20");    // 20% - tỉ lệ chấp nhận
    private static final BigDecimal WEIGHT_WORKLOAD = new BigDecimal("0.15");      // 15% - tải việc

    // Cấu hình timeout
    private static final int MATCHING_TIMEOUT_MINUTES = 5;
    private static final int MAX_WORKERS_PER_REQUEST = 5;
    private static final int MAX_DISTANCE_KM = 20;

    @Transactional
    public MatchingResultDTO findAndMatchWorkers(Long requestId) {
        RepairRequest request = repairRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu"));

        if (request.getStatus() != RepairStatus.PENDING_MATCH && request.getStatus() != RepairStatus.MATCHING) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Yêu cầu không ở trạng thái chờ ghép thợ");
        }

        // Kiểm tra tọa độ
        if (request.getLat() == null || request.getLng() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Yêu cầu thiếu tọa độ vị trí");
        }

        // Cập nhật trạng thái đang matching
        request.setStatus(RepairStatus.MATCHING);
        request.setUpdatedAt(LocalDateTime.now());

        // Tìm thợ khả dụng
        List<WorkerProfile> availableWorkers = findAvailableWorkers(request);
        if (availableWorkers.isEmpty()) {
            request.setStatus(RepairStatus.NOT_FOUND);
            return MatchingResultDTO.builder()
                    .success(false)
                    .message("Không tìm thấy thợ phù hợp")
                    .matchedWorkers(Collections.emptyList())
                    .build();
        }

        // Tính toán điểm cho từng thợ
        List<WorkerScoreDTO> scoredWorkers = calculateScores(request, availableWorkers);
        
        // Lọc thợ trong khoảng cách cho phép
        List<WorkerScoreDTO> eligibleWorkers = scoredWorkers.stream()
                .filter(w -> w.getDistanceKm() <= MAX_DISTANCE_KM)
                .limit(MAX_WORKERS_PER_REQUEST)
                .toList();

        if (eligibleWorkers.isEmpty()) {
            request.setStatus(RepairStatus.NOT_FOUND);
            return MatchingResultDTO.builder()
                    .success(false)
                    .message("Không có thợ nào trong khoảng cách cho phép")
                    .matchedWorkers(Collections.emptyList())
                    .build();
        }

        // Cập nhật trạng thái request
        request.setStatus(RepairStatus.MATCHING);
        
        // Gửi yêu cầu đến các thợ theo thứ tự điểm
        List<WorkerMatchDTO> matchedWorkers = new ArrayList<>();
        for (WorkerScoreDTO scored : eligibleWorkers) {
            MatchingLog log = createMatchingLog(request, scored.getWorker(), scored);
            matchingLogRepository.save(log);

            // Gửi notification đến thợ
            notificationService.sendMatchingRequest(scored.getWorker().getUser().getId(), request.getId(), log.getId());

            matchedWorkers.add(WorkerMatchDTO.builder()
                    .workerId(scored.getWorker().getId())
                    .workerName(scored.getWorker().getUser().getFullName())
                    .distanceKm(scored.getDistanceKm())
                    .durationMinutes(scored.getDurationMinutes())
                    .totalScore(scored.getTotalScore())
                    .matchingLogId(log.getId())
                    .build());
        }

        return MatchingResultDTO.builder()
                .success(true)
                .message("Đã gửi yêu cầu đến " + matchedWorkers.size() + " thợ")
                .matchedWorkers(matchedWorkers)
                .build();
    }

    private List<WorkerProfile> findAvailableWorkers(RepairRequest request) {
        // Tìm thợ theo chuyên môn, trạng thái approved, ready, active
        List<WorkerProfile> workers = workerProfileRepository.findAvailableWorkers(
                ApprovalStatus.APPROVED,
                AvailabilityStatus.READY,
                request.getCategory().getId(),
                request.getAddress() != null ? request.getAddress().getFullAddress() : null
        );

        // Lọc thêm: thợ phải có tọa độ (để tính khoảng cách)
        return workers.stream()
                .filter(w -> w.getUser() != null)
                .filter(w -> w.getUser().getLat() != null && w.getUser().getLng() != null)
                .toList();
    }

    private List<WorkerScoreDTO> calculateScores(RepairRequest request, List<WorkerProfile> workers) {
        // Lấy tọa độ customer
        GoongService.Coordinate customerCoord = new GoongService.Coordinate(
                request.getLat().doubleValue(),
                request.getLng().doubleValue()
        );

        // Lấy tọa độ các thợ
        List<GoongService.Coordinate> workerCoords = workers.stream()
                .map(w -> new GoongService.Coordinate(w.getUser().getLat().doubleValue(), w.getUser().getLng().doubleValue()))
                .toList();

        // Gọi Goong Distance Matrix API
        GoongService.DistanceMatrixResponse distanceMatrix = goongService.getDistanceMatrix(
                List.of(GoongService.Coordinate.from(request.getLat(), request.getLng())),
                workerCoords,
                "motorcycle" // Thợ thường đi xe máy
        );

        List<WorkerScoreDTO> scoredWorkers = new ArrayList<>();

        for (int i = 0; i < workers.size() && i < distanceMatrix.getRows().get(0).getElements().size(); i++) {
            WorkerProfile worker = workers.get(i);
            GoongService.DistanceMatrixResponse.Element element = 
                    distanceMatrix.getRows().get(0).getElements().get(i);

            if (!"OK".equals(element.getStatus()) || element.getDistance() == null) {
                log.warn("Không tính được khoảng cách cho thợ {}", worker.getId());
                continue;
            }

            double distanceKm = element.getDistance().getValue() / 1000.0;
            int durationMinutes = (int) Math.ceil(element.getDuration().getValue() / 60.0);

            // Tính các thành phần điểm
            BigDecimal distanceScore = calculateDistanceScore(distanceKm);
            BigDecimal ratingScore = calculateRatingScore(worker.getAverageRating());
            BigDecimal acceptanceScore = calculateAcceptanceRateScore(worker.getAcceptanceRate());
            BigDecimal workloadScore = calculateWorkloadScore(worker.getOngoingJobsCount());

            // Tính tổng điểm có trọng số
            BigDecimal totalScore = distanceScore.multiply(WEIGHT_DISTANCE)
                    .add(ratingScore.multiply(WEIGHT_RATING))
                    .add(acceptanceScore.multiply(WEIGHT_ACCEPTANCE))
                    .add(workloadScore.multiply(WEIGHT_WORKLOAD))
                    .setScale(4, RoundingMode.HALF_UP);

            scoredWorkers.add(WorkerScoreDTO.builder()
                    .worker(workers.get(i))
                    .distanceKm(element.getDistance().getValue() / 1000.0)
                    .durationMinutes((int) Math.ceil(element.getDuration().getValue() / 60.0))
                    .distanceScore(distanceScore)
                    .ratingScore(ratingScore)
                    .acceptanceScore(acceptanceScore)
                    .workloadScore(workloadScore)
                    .totalScore(totalScore)
                    .build());
        }

        // Sắp xếp giảm dần theo totalScore
        return scoredWorkers.stream()
                .sorted(Comparator.comparing(WorkerScoreDTO::getTotalScore).reversed())
                .toList();
    }

    private BigDecimal calculateDistanceScore(double distanceKm) {
        // Càng gần càng điểm cao: 100 điểm tại 0km, giảm dần đến 0 tại 20km
        if (distanceKm <= 1) return BigDecimal.valueOf(100);
        if (distanceKm >= MAX_DISTANCE_KM) return BigDecimal.ZERO;
        return BigDecimal.valueOf(100 * (1 - (distanceKm - 1) / (MAX_DISTANCE_KM - 1)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRatingScore(BigDecimal avgRating) {
        // Rating 5.0 = 100, 4.0 = 80, 3.0 = 60, ...
        if (avgRating == null) return BigDecimal.valueOf(50);
        return BigDecimal.valueOf(avgRating.doubleValue() * 20).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAcceptanceRateScore(BigDecimal acceptanceRate) {
        // Acceptance rate 100% = 100, 80% = 80, 50% = 50
        if (acceptanceRate == null) return BigDecimal.valueOf(50);
        return acceptanceRate.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWorkloadScore(int ongoingJobs) {
        // 0 việc = 100, 1 = 80, 2 = 60, 3+ = 40
        if (ongoingJobs == 0) return BigDecimal.valueOf(100);
        if (ongoingJobs == 1) return BigDecimal.valueOf(80);
        if (ongoingJobs == 2) return BigDecimal.valueOf(60);
        return BigDecimal.valueOf(40);
    }

    private MatchingLog createMatchingLog(RepairRequest request, WorkerProfile worker, WorkerScoreDTO score) {
        return MatchingLog.builder()
                .request(request)
                .worker(worker)
                .totalScore(score.getTotalScore())
                .distanceScore(score.getDistanceScore())
                .ratingScore(score.getRatingScore())
                .acceptanceRateScore(score.getAcceptanceScore())
                .workloadScore(score.getWorkloadScore())
                .result(MatchingResult.PENDING)
                .sentAt(LocalDateTime.now())
                .build();
    }

    // Scheduled task: kiểm tra timeout matching
    @Scheduled(fixedRate = 60000) // Mỗi phút
    @Transactional
    public void checkMatchingTimeouts() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(MATCHING_TIMEOUT_MINUTES);
        
        List<MatchingLog> timedOut = matchingLogRepository.findByResultAndSentAtBefore(
                MatchingResult.PENDING, 
                LocalDateTime.now().minusMinutes(MATCHING_TIMEOUT_MINUTES)
        );

        for (MatchingLog log : timedOut) {
            handleTimeout(log);
        }
    }

    @Transactional
    public void handleWorkerResponse(Long matchingLogId, Long workerId, MatchingResult result, String note) {
        MatchingLog log = matchingLogRepository.findById(matchingLogId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy log matching"));

        if (!log.getWorker().getId().equals(workerId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Không có quyền thực hiện");
        }

        if (log.getResult() != MatchingResult.PENDING) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Yêu cầu này đã được xử lý");
        }

        log.setResult(result);
        log.setRespondedAt(LocalDateTime.now());
        log.setNote(note);
        matchingLogRepository.save(log);

        RepairRequest request = log.getRequest();

        if (result == MatchingResult.ACCEPTED) {
            // Thợ chấp nhận
            request.setWorker(log.getWorker());
            request.setStatus(RepairStatus.MATCHED);
            request.setUpdatedAt(LocalDateTime.now());
            
            // Hủy các matching log khác của request này
            matchingLogRepository.findByRequestIdAndResultList(request.getId(), MatchingResult.PENDING)
                    .forEach(logItem -> {
                        logItem.setResult(MatchingResult.CANCELLED_MIDWAY);
                        logItem.setRespondedAt(LocalDateTime.now());
                        logItem.setNote("Thợ khác đã chấp nhận");
                    });
            matchingLogRepository.saveAll(matchingLogRepository.findByRequestIdAndResultList(request.getId(), MatchingResult.PENDING));
            
            // Thông báo cho khách
            notificationService.sendWorkerAccepted(request.getCustomer().getId(), request.getId(), log.getWorker().getUser().getFullName());
            
        } else if (result == MatchingResult.REJECTED) {
            // Thợ từ chối - chuyển sang thợ tiếp theo
            log.setResult(MatchingResult.REJECTED);
            log.setRespondedAt(LocalDateTime.now());
            matchingLogRepository.save(log);

            // Tìm thợ tiếp theo
            findNextWorker(log.getRequest());
        }

        matchingLogRepository.save(log);
    }

    private void findNextWorker(RepairRequest request) {
        // Lấy các thợ đã được thử
        List<Long> triedWorkerIds = matchingLogRepository.findByRequestId(request.getId(), Pageable.unpaged())
                .stream()
                .map(log -> log.getWorker().getId())
                .toList();

        // Tìm thợ mới chưa được thử
        List<WorkerProfile> workers = findAvailableWorkers(repairRequestRepository.getReferenceById(request.getId()));
        workers = workers.stream()
                .filter(w -> !triedWorkerIds.contains(w.getId()))
                .toList();

        if (workers.isEmpty()) {
            request.setStatus(RepairStatus.NOT_FOUND);
            request.setUpdatedAt(LocalDateTime.now());
            repairRequestRepository.save(request);
            notificationService.sendNoWorkerFound(request.getCustomer().getId(), request.getId());
            return;
        }

        // Tính điểm cho thợ tiếp theo
        RepairRequest requestEntity = repairRequestRepository.getReferenceById(request.getId());
        List<WorkerScoreDTO> scored = calculateScores(requestEntity, workers);
        
        Optional<WorkerScoreDTO> nextWorker = scored.stream()
                .filter(w -> w.getDistanceKm() <= MAX_DISTANCE_KM)
                .findFirst();

        if (nextWorker.isPresent()) {
            WorkerScoreDTO next = nextWorker.get();
            MatchingLog log = createMatchingLog(request, next.getWorker(), next);
            matchingLogRepository.save(log);
            notificationService.sendMatchingRequest(next.getWorker().getUser().getId(), request.getId(), log.getId());
        } else {
            request.setStatus(RepairStatus.NOT_FOUND);
            request.setUpdatedAt(LocalDateTime.now());
        }
    }

    private void handleTimeout(MatchingLog log) {
        log.setResult(MatchingResult.EXPIRED);
        log.setRespondedAt(LocalDateTime.now());
        log.setNote("Thợ không phản hồi trong thời gian chờ");
        matchingLogRepository.save(log);

        // Tìm thợ tiếp theo
        findNextWorker(log.getRequest());
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WorkerScoreDTO {
        private WorkerProfile worker;
        private double distanceKm;
        private int durationMinutes;
        private BigDecimal distanceScore;
        private BigDecimal ratingScore;
        private BigDecimal acceptanceScore;
        private BigDecimal workloadScore;
        private BigDecimal totalScore;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WorkerMatchDTO {
        private Long workerId;
        private String workerName;
        private double distanceKm;
        private int durationMinutes;
        private BigDecimal totalScore;
        private Long matchingLogId;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MatchingResultDTO {
        private boolean success;
        private String message;
        private List<WorkerMatchDTO> matchedWorkers;
    }
}