package com.thonha.backend.security;

import com.thonha.backend.repository.RepairRequestRepository;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xác thực + phân quyền cho kênh WebSocket (STOMP).
 * Trình duyệt không gửi được header Authorization lúc bắt tay WebSocket, nên JWT được gửi trong frame CONNECT.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {
    private static final Pattern REQUEST_TOPIC = Pattern.compile("^/topic/requests/(\\d+)/(location|chat)$");

    private final JwtService jwt;
    private final RepairRequestRepository requests;

    public StompAuthInterceptor(JwtService jwt, RepairRequestRepository requests) {
        this.jwt = jwt;
        this.requests = requests;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null || acc.getCommand() == null) return message;

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            acc.setUser(authenticate(acc.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(acc.getCommand())) {
            authorizeSubscribe(acc);
        } else if (StompCommand.SEND.equals(acc.getCommand()) && acc.getUser() == null) {
            throw new MessagingException("Unauthenticated");
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) throw new MessagingException("Missing token");
        try {
            Claims c = jwt.parse(header.substring(7));
            if (!"access".equals(c.get("type", String.class))) throw new MessagingException("Invalid token type");
            Long userId = Long.valueOf(c.getSubject());
            Collection<?> roles = c.get("roles") instanceof Collection<?> col ? col : List.of();
            var authorities = roles.stream().map(Object::toString).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
            return new UsernamePasswordAuthenticationToken(userId, null, authorities);
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            throw new MessagingException("Invalid or expired token");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor acc) {
        if (!(acc.getUser() instanceof Authentication auth)) throw new MessagingException("Unauthenticated");
        String dest = acc.getDestination();
        if (dest == null) throw new MessagingException("Missing destination");

        // Hàng đợi riêng của từng user (nhận thông báo lỗi)
        if (dest.startsWith("/user/queue/")) return;

        Matcher m = REQUEST_TOPIC.matcher(dest);
        if (!m.matches()) throw new MessagingException("Subscription not allowed");

        Long userId = Long.valueOf(auth.getName());
        Long requestId = Long.valueOf(m.group(1));
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean allowed = admin
                ? requests.existsById(requestId)
                : requests.existsByIdAndCustomerId(requestId, userId) || requests.existsByIdAndWorker_User_Id(requestId, userId);
        if (!allowed) throw new MessagingException("Not allowed to follow this request");
    }
}
