package com.thonha.backend.controller;

import com.thonha.backend.dto.tracking.LocationMessage;
import com.thonha.backend.service.TrackingService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class TrackingWsController {
    private final TrackingService tracking;

    public TrackingWsController(TrackingService tracking) {
        this.tracking = tracking;
    }

    /** Thợ gửi vị trí: SEND /app/requests/{id}/location */
    @MessageMapping("/requests/{id}/location")
    public void location(@DestinationVariable("id") Long id, @Payload LocationMessage msg, Principal principal) {
        tracking.handleLocation(id, Long.valueOf(principal.getName()), msg);
    }

    /** Lỗi (không được ghép, sai trạng thái, toạ độ sai...) trả riêng cho người gửi ở /user/queue/errors */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String onError(Exception e) {
        return e.getMessage();
    }
}
