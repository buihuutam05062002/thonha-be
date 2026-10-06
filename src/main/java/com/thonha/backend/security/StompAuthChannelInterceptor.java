package com.thonha.backend.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bảo vệ kênh STOMP:
 *  - CONNECT:   bắt buộc có JWT access token hợp lệ, gắn userId vào session.
 *  - SUBSCRIBE: chỉ được nghe /topic/user/{userId}/... của CHÍNH MÌNH
 *               (nếu không, ai đăng nhập cũng nghe lén được thông báo của người khác).
 *  - SEND:      chặn hoàn toàn — kênh này chỉ để server đẩy xuống client.
 *
 * Lưu ý: token chỉ được kiểm tra lúc CONNECT. Kết nối đang mở vẫn sống sau khi token hết hạn;
 * khi kết nối lại, FE sẽ gửi token mới.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern USER_TOPIC = Pattern.compile("^/topic/user/(\\d+)/[\\w-]+$");

    private final JwtService jwt;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            throw new MessageDeliveryException("Client không được phép gửi message qua WebSocket");
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Thiếu access token");
        }
        try {
            Claims claims = jwt.parse(header.substring(7));
            if (!"access".equals(claims.get("type", String.class))) {
                throw new MessageDeliveryException("Token không hợp lệ");
            }
            String userId = String.valueOf(Long.valueOf(claims.getSubject()));
            Principal principal = () -> userId;
            accessor.setUser(principal);
        } catch (MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.debug("STOMP CONNECT bị từ chối: {}", e.getMessage());
            throw new MessageDeliveryException("Token không hợp lệ hoặc đã hết hạn");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (user == null || destination == null) {
            throw new MessageDeliveryException("Chưa xác thực");
        }
        Matcher m = USER_TOPIC.matcher(destination);
        if (!m.matches() || !m.group(1).equals(user.getName())) {
            throw new MessageDeliveryException("Không có quyền đăng ký kênh này");
        }
    }
}
