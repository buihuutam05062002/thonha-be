package com.thonha.backend.dto.chat;

import com.thonha.backend.entity.Message;

import java.time.LocalDateTime;

public record MessageResponse(Long id, Long requestId, Long senderId, String senderName,
                              String senderAvatarUrl, String content, boolean seen, LocalDateTime sentAt) {
    public static MessageResponse from(Message m) {
        var u = m.getSender();
        return new MessageResponse(m.getId(), m.getRequest().getId(), u.getId(), u.getFullName(),
                u.getAvatarUrl(), m.getContent(), m.isSeen(), m.getSentAt());
    }
}