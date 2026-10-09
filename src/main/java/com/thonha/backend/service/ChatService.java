package com.thonha.backend.service;

import com.thonha.backend.dto.chat.MessageResponse;
import com.thonha.backend.entity.Message;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.entity.User;
import com.thonha.backend.common.NotFoundException;
import com.thonha.backend.common.ForbiddenException;
import com.thonha.backend.repository.MessageRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private final RepairRequestRepository requests;
    private final UserRepository users;
    private final MessageRepository messages;
    private final SimpMessagingTemplate broker;

    public ChatService(RepairRequestRepository requests, UserRepository users,
                       MessageRepository messages, SimpMessagingTemplate broker) {
        this.requests = requests;
        this.users = users;
        this.messages = messages;
        this.broker = broker;
    }

    /** Gọi từ cả REST (dự phòng) lẫn WS (realtime) — xem controller bên dưới. */
    @Transactional
    public MessageResponse send(Long requestId, Long senderId, String content) {
        RepairRequest r = requireAccess(requestId, senderId);
        User sender = users.findById(senderId).orElseThrow(() -> new NotFoundException("User not found"));

        Message m = new Message();
        m.setRequest(r);
        m.setSender(sender);
        m.setContent(content.trim());
        messages.save(m);

        MessageResponse res = MessageResponse.from(m);
        // Phát cho CẢ 2 phía đang subscribe cùng topic (kể cả người vừa gửi, để đồng bộ nếu họ mở 2 tab)
        broker.convertAndSend("/topic/requests/" + requestId + "/chat", res);
        return res;
    }

    /**
     * Lịch sử theo trang (phân trang theo con trỏ):
     * - before == null: lấy {limit} tin MỚI NHẤT (lúc mở khung chat) và đánh dấu đã đọc.
     * - before = id: lấy {limit} tin ngay TRƯỚC tin có id đó (khi bấm "tải tin cũ hơn").
     * Kết quả luôn xếp cũ -> mới. Không truyền cả hai tham số thì giữ hành vi cũ (trả toàn bộ).
     */
    @Transactional
    public List<MessageResponse> history(Long requestId, Long viewerId, Long before, Integer limit) {
        if (before == null && limit == null) {
            return history(requestId, viewerId);
        }
        requireAccess(requestId, viewerId);
        if (before == null) {
            messages.markSeen(requestId, viewerId);
        }
        int size = Math.max(1, Math.min(limit == null ? DEFAULT_PAGE_SIZE : limit, MAX_PAGE_SIZE));
        PageRequest page = PageRequest.of(0, size);
        List<Message> rows = before == null
                ? messages.findByRequest_IdOrderByIdDesc(requestId, page)
                : messages.findByRequest_IdAndIdLessThanOrderByIdDesc(requestId, before, page);
        List<MessageResponse> out = new ArrayList<>(rows.stream().map(MessageResponse::from).toList());
        Collections.reverse(out);
        return out;
    }

    @Transactional
    public List<MessageResponse> history(Long requestId, Long viewerId) {
        requireAccess(requestId, viewerId);
        messages.markSeen(requestId, viewerId);
        return messages.findByRequest_IdOrderBySentAtAsc(requestId).stream().map(MessageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unseenCount(Long requestId, Long viewerId) {
        requireAccess(requestId, viewerId);
        return messages.countByRequest_IdAndSender_IdNotAndSeenFalse(requestId, viewerId);
    }

    /**
     * Chỉ khách hàng của đơn hoặc thợ ĐANG ĐƯỢC GHÉP cho đơn mới được chat.
     * Tự nhiên chặn luôn trường hợp "thợ chưa được ghép" vì r.getWorker() lúc đó là null.
     */
    private RepairRequest requireAccess(Long requestId, Long userId) {
        RepairRequest r = requests.findById(requestId).orElseThrow(() -> new NotFoundException("Repair request not found"));
        boolean isCustomer = r.getCustomer().getId().equals(userId);
        boolean isWorker = r.getWorker() != null && r.getWorker().getUser().getId().equals(userId);
        if (!isCustomer && !isWorker) throw new ForbiddenException("You don't have access to this conversation");
        return r;
    }
}