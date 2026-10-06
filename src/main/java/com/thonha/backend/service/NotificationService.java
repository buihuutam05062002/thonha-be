package com.thonha.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    private SimpMessagingTemplate messagingTemplate;

    public NotificationService() {}

    @Autowired(required = false)
    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendMatchingRequest(Long workerUserId, Long requestId, Long matchingLogId) {
        if (messagingTemplate == null) {
            log.debug("WebSocket not enabled, skipping sendMatchingRequest to user {}", workerUserId);
            return;
        }
        String destination = "/topic/user/" + workerUserId + "/matching";
        MatchingNotification notification = MatchingNotification.builder()
                .type("MATCHING_REQUEST")
                .requestId(requestId)
                .matchingLogId(matchingLogId)
                .message("Bạn có yêu cầu sửa chữa mới")
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(destination, notification);
        log.debug("Sent matching request to user {}: requestId={}, matchingLogId={}", 
                workerUserId, requestId, matchingLogId);
    }

    public void sendWorkerAccepted(Long customerUserId, Long requestId, String workerName) {
        if (messagingTemplate == null) {
            log.debug("WebSocket not enabled, skipping sendWorkerAccepted to user {}", customerUserId);
            return;
        }
        String destination = "/topic/user/" + customerUserId + "/requests";
        MatchingNotification notification = MatchingNotification.builder()
                .type("WORKER_ACCEPTED")
                .requestId(requestId)
                .message("Thợ " + workerName + " đã chấp nhận yêu cầu của bạn")
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(destination, notification);
    }

    public void sendWorkerRejected(Long customerUserId, Long requestId, String workerName) {
        if (messagingTemplate == null) {
            log.debug("WebSocket not enabled, skipping sendWorkerRejected to user {}", customerUserId);
            return;
        }
        String destination = "/topic/user/" + customerUserId + "/requests";
        MatchingNotification notification = MatchingNotification.builder()
                .type("WORKER_REJECTED")
                .requestId(requestId)
                .message("Thợ " + workerName + " đã từ chối yêu cầu của bạn")
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(destination, notification);
    }

    public void sendNoWorkerFound(Long customerUserId, Long requestId) {
        if (messagingTemplate == null) {
            log.debug("WebSocket not enabled, skipping sendNoWorkerFound to user {}", customerUserId);
            return;
        }
        String destination = "/topic/user/" + customerUserId + "/requests";
        MatchingNotification notification = MatchingNotification.builder()
                .type("NO_WORKER_FOUND")
                .requestId(requestId)
                .message("Không tìm thấy thợ phù hợp cho yêu cầu của bạn")
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(destination, notification);
    }

    public void sendWorkerArrived(Long customerUserId, Long requestId) {
        if (messagingTemplate == null) {
            log.debug("WebSocket not enabled, skipping sendWorkerArrived to user {}", customerUserId);
            return;
        }
        String destination = "/topic/user/" + customerUserId + "/requests";
        MatchingNotification notification = MatchingNotification.builder()
                .type("WORKER_ARRIVED")
                .requestId(requestId)
                .message("Thợ đã đến nơi")
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(destination, notification);
    }

    public void sendJobCompleted(Long customerUserId, Long requestId) {
        String destination = "/topic/user/" + customerUserId + "/requests";
        MatchingNotification notification = MatchingNotification.builder()
                .type("JOB_COMPLETED")
                .requestId(requestId)
                .message("Yêu cầu sửa chữa đã hoàn thành")
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(destination, notification);
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