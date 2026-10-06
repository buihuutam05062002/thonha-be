package com.thonha.backend.controller;

import com.thonha.backend.dto.chat.SendMessageRequest;
import com.thonha.backend.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWsController {
    private final ChatService chat;

    public ChatWsController(ChatService chat) {
        this.chat = chat;
    }

    /** Gửi tin nhắn: SEND /app/requests/{id}/chat -> lưu DB -> phát lại /topic/requests/{id}/chat cho cả 2 phía. */
    @MessageMapping("/requests/{id}/chat")
    public void send(@DestinationVariable("id") Long id, @Payload SendMessageRequest msg, Principal principal) {
        chat.send(id, Long.valueOf(principal.getName()), msg.content());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String onError(Exception e) {
        return e.getMessage();
    }
}