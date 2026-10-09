package com.thonha.backend.security;

import com.thonha.backend.repository.RepairRequestRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bảo vệ kênh STOMP (một interceptor dùng chung cho thông báo, theo dõi vị trí và chat):
 *
 *  - CONNECT:   bắt buộc có JWT access token hợp lệ (header "Authorization: Bearer ..."), gắn userId + roles vào session.
 *  - SUBSCRIBE: chỉ được nghe
 *                 /topic/user/{userId}/...            của CHÍNH MÌNH (thông báo ghép thợ, trạng thái yêu cầu)
 *                 /topic/requests/{id}/location|chat  nếu là khách của yêu cầu, thợ được ghép, hoặc admin
 *                 /user/queue/...                     hàng đợi riêng (thông báo lỗi)
 *  - SEND:      chỉ được gửi tới /app/requests/{id}/location|chat và phải là khách/thợ của yêu cầu đó.
 *               Kiểm tra chi tiết hơn (đúng thợ được ghép, đúng trạng thái) nằm ở TrackingService / ChatService.
 *
 * Lưu ý: token chỉ được kiểm tra lúc CONNECT. Kết nối đang mở vẫn sống sau khi token hết hạn;
 * khi kết nối lại, FE sẽ gửi token mới.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private static final Pattern USER_TOPIC = Pattern.compile("^/topic/user/(\\d+)/[\\w-]+$");
    private static final Pattern REQUEST_TOPIC = Pattern.compile("^/topic/requests/(\\d+)/(location|chat)$");
    private static final Pattern REQUEST_SEND = Pattern.compile("^/app/requests/(\\d+)/(location|chat)$");

    private final JwtService jwt;
    private final RepairRequestRepository requests;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            authorizeSend(accessor);
        }
        return message;
    }

    private Authentication authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Thiếu access token");
        }
        try {
            Claims claims = jwt.parse(header.substring(7));
            if (!"access".equals(claims.get("type", String.class))) {
                throw new MessageDeliveryException("Token không hợp lệ");
            }
            Long userId = Long.valueOf(claims.getSubject());
            Collection<?> roles = claims.get("roles") instanceof Collection<?> c ? c : List.of();
            var authorities = roles.stream()
                    .map(Object::toString)
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            return new UsernamePasswordAuthenticationToken(userId, null, authorities);
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
        // Hàng đợi riêng của từng user (nhận thông báo lỗi)
        if (destination.startsWith("/user/queue/")) {
            return;
        }
        Matcher own = USER_TOPIC.matcher(destination);
        if (own.matches()) {
            if (!own.group(1).equals(user.getName())) {
                throw new MessageDeliveryException("Không có quyền đăng ký kênh này");
            }
            return;
        }
        Matcher req = REQUEST_TOPIC.matcher(destination);
        if (req.matches() && canAccessRequest(user, Long.valueOf(req.group(1)))) {
            return;
        }
        throw new MessageDeliveryException("Không có quyền đăng ký kênh này");
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (user == null || destination == null) {
            throw new MessageDeliveryException("Chưa xác thực");
        }
        Matcher m = REQUEST_SEND.matcher(destination);
        // gửi vị trí / chat chỉ dành cho khách hoặc thợ của yêu cầu (admin chỉ được xem, không gửi)
        if (!m.matches() || !isParticipant(Long.valueOf(user.getName()), Long.valueOf(m.group(1)))) {
            throw new MessageDeliveryException("Client không được phép gửi message tới kênh này");
        }
    }

    private boolean canAccessRequest(Principal user, Long requestId) {
        boolean admin = user instanceof Authentication auth
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return admin ? requests.existsById(requestId) : isParticipant(Long.valueOf(user.getName()), requestId);
    }

    private boolean isParticipant(Long userId, Long requestId) {
        return requests.existsByIdAndCustomerId(requestId, userId)
                || requests.existsByIdAndWorker_User_Id(requestId, userId);
    }
}
