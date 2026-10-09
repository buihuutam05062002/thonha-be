package com.thonha.backend.config;

import com.thonha.backend.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket - một endpoint dùng chung cho mọi tính năng realtime.
 *
 * - Endpoint:  ws://host:8080/ws  (WebSocket thuần, không SockJS)
 * - Broker:    /topic/**, /queue/**  (simple in-memory broker, chạy 1 instance là đủ)
 * - Client gửi lên (prefix /app):
 *       /app/requests/{id}/location   thợ gửi vị trí          (TrackingWsController)
 *       /app/requests/{id}/chat       gửi tin nhắn            (ChatWsController)
 * - Server đẩy xuống:
 *       /topic/user/{userId}/matching|requests   thông báo ghép thợ / trạng thái yêu cầu (NotificationService)
 *       /topic/requests/{id}/location            vị trí thợ realtime                      (TrackingService)
 *       /topic/requests/{id}/chat                tin nhắn chat                            (ChatService)
 *       /user/queue/errors                       lỗi riêng cho người gửi
 * - Xác thực:  JWT gửi trong STOMP CONNECT header "Authorization: Bearer ..."
 *              (trình duyệt không gắn được header lên handshake HTTP) -> xem StompAuthChannelInterceptor
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompAuthChannelInterceptor authInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Heartbeat 2 chiều 10s để server/FE phát hiện kết nối chết (đóng laptop, rớt mạng) và dọn session.
        // Simple broker cần TaskScheduler riêng cho việc này.
        ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-");
        heartbeatScheduler.setDaemon(true);
        heartbeatScheduler.initialize();
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10_000, 10_000})
                .setTaskScheduler(heartbeatScheduler);
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // giữ đồng bộ với CorsConfig
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
