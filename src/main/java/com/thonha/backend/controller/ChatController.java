package com.thonha.backend.controller;

import com.thonha.backend.dto.chat.MessageResponse;
import com.thonha.backend.dto.chat.SendMessageRequest;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ChatController {
    private final ChatService s;
    private final CurrentUserProvider c;

    public ChatController(ChatService s, CurrentUserProvider c) {
        this.s = s;
        this.c = c;
    }

    /** Mở màn chat sẽ gọi cái này để tải lịch sử; đồng thời đánh dấu đã đọc tin của phía kia. */
    @GetMapping("/repair-requests/{id}/messages")
    List<MessageResponse> history(@PathVariable Long id,
                                  @RequestParam(required = false) Long before,
                                  @RequestParam(required = false) Integer limit) {
        return s.history(id, c.requireUserId(), before, limit);
    }

    /** Gửi qua REST — dự phòng khi WS chưa kết nối được. Khuyến khích dùng WS /app/requests/{id}/chat để realtime. */
    @PostMapping("/repair-requests/{id}/messages")
    MessageResponse send(@PathVariable Long id, @Valid @RequestBody SendMessageRequest req) {
        return s.send(id, c.requireUserId(), req.content());
    }

    @GetMapping("/repair-requests/{id}/messages/unseen-count")
    Map<String, Long> unseenCount(@PathVariable Long id) {
        return Map.of("count", s.unseenCount(id, c.requireUserId()));
    }
}