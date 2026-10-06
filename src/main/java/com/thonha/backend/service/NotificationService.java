package com.thonha.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
public class NotificationService {

    private SimpMessagingTemplate messagingTemplate;

    public NotificationService() {}

    @Autowired(required = false)
    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Đẩy thông báo tới /topic/**.
     * - Nếu đang trong transaction: chỉ gửi SAU KHI COMMIT. Nếu gửi ngay, FE nhận push và gọi lại API
     *   trước khi dữ liệu được commit -> thấy dữ liệu cũ (hoặc thấy yêu cầu đã bị rollback).
     * - Lỗi WebSocket không bao giờ được làm hỏng nghiệp vụ chính -> chỉ log.
     */
    private void push(String destination, MatchingNotification notification) {
        if (messagingTemplate == null) {
            log.debug("WebSocket not enabled, skipping {} -> {}", notification.getType(), destination);
            return;
        }
        Runnable send = () -> {
            try {
                messagingTemplate.convertAndSend(destination, notification);
                log.debug("Sent {} to {}: requestId={}", notification.getType(), destination, notification.getRequestId());
            } catch (Exception e) {
                log.warn("Gửi WebSocket thất bại ({} -> {}): {}", notification.getType(), destination, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private MatchingNotification.MatchingNotificationBuilder base(String type, Long requestId, String message) {
        return MatchingNotification.builder()
                .type(type)
                .requestId(requestId)
                .message(message)
                .timestamp(System.currentTimeMillis());
    }

    // ---------------------------------------------------------------- gửi cho THỢ  (/topic/user/{id}/matching)

    public void sendMatchingRequest(Long workerUserId, Long requestId, Long matchingLogId) {
        push("/topic/user/" + workerUserId + "/matching",
                base("MATCHING_REQUEST", requestId, "Bạn có yêu cầu sửa chữa mới")
                        .matchingLogId(matchingLogId)
                        .build());
    }

    /** Yêu cầu không còn dành cho thợ này nữa (thợ khác đã nhận / hết hạn / khách hủy) -> FE tải lại dashboard. */
    public void sendMatchingClosed(Long workerUserId, Long requestId) {
        push("/topic/user/" + workerUserId + "/matching",
                base("MATCHING_CLOSED", requestId, "Yêu cầu không còn khả dụng").build());
    }

    /** Khách hủy đơn thợ đã nhận. */
    public void sendRequestCancelled(Long workerUserId, Long requestId) {
        push("/topic/user/" + workerUserId + "/matching",
                base("REQUEST_CANCELLED", requestId, "Khách đã hủy yêu cầu").build());
    }

    // ---------------------------------------------------------------- gửi cho KHÁCH (/topic/user/{id}/requests)

    public void sendWorkerAccepted(Long customerUserId, Long requestId, String workerName) {
        push("/topic/user/" + customerUserId + "/requests",
                base("WORKER_ACCEPTED", requestId, "Thợ " + workerName + " đã chấp nhận yêu cầu của bạn").build());
    }

    public void sendWorkerRejected(Long customerUserId, Long requestId, String workerName) {
        push("/topic/user/" + customerUserId + "/requests",
                base("WORKER_REJECTED", requestId, "Thợ " + workerName + " đã từ chối yêu cầu của bạn").build());
    }

    public void sendNoWorkerFound(Long customerUserId, Long requestId) {
        push("/topic/user/" + customerUserId + "/requests",
                base("NO_WORKER_FOUND", requestId, "Không tìm thấy thợ phù hợp cho yêu cầu của bạn").build());
    }

    public void sendWorkerArrived(Long customerUserId, Long requestId) {
        push("/topic/user/" + customerUserId + "/requests",
                base("WORKER_ARRIVED", requestId, "Thợ đã đến nơi").build());
    }

    public void sendJobCompleted(Long customerUserId, Long requestId) {
        push("/topic/user/" + customerUserId + "/requests",
                base("JOB_COMPLETED", requestId, "Yêu cầu sửa chữa đã hoàn thành").build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MatchingNotification {
        private String type;
        private Long requestId;
        private Long matchingLogId;
        private String message;
        private Long timestamp;
    }
}
