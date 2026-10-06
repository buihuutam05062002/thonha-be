package com.thonha.backend.controller;

import com.thonha.backend.dto.tracking.ActiveJob;
import com.thonha.backend.dto.tracking.TrackingSnapshot;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.TrackingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TrackingController {
    private final TrackingService s;
    private final CurrentUserProvider c;

    public TrackingController(TrackingService s, CurrentUserProvider c) {
        this.s = s;
        this.c = c;
    }

    /** Khách mở trang theo dõi: lấy trạng thái, thông tin thợ và vị trí gần nhất. */
    @GetMapping("/repair-requests/{id}/tracking")
    TrackingSnapshot tracking(@PathVariable Long id) {
        return s.snapshot(id, c.requireUserId());
    }

    /** Thợ: các đơn đang thực hiện để chia sẻ vị trí. */
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/worker/jobs/active")
    List<ActiveJob> activeJobs() {
        return s.activeJobs(c.requireUserId());
    }
}
