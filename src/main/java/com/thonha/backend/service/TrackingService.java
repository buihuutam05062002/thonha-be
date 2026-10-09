package com.thonha.backend.service;

import com.thonha.backend.dto.tracking.ActiveJob;
import com.thonha.backend.dto.tracking.LocationMessage;
import com.thonha.backend.dto.tracking.TrackingEvent;
import com.thonha.backend.dto.tracking.TrackingSnapshot;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.entity.User;
import com.thonha.backend.common.BadRequestException;
import com.thonha.backend.common.NotFoundException;
import com.thonha.backend.common.ForbiddenException;
import com.thonha.backend.repository.RepairRequestRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrackingService {
    /** Chỉ theo dõi vị trí khi thợ đã được ghép và chưa bắt đầu sửa. */
    private static final List<RepairStatus> TRACKABLE = List.of(RepairStatus.MATCHED, RepairStatus.ON_THE_WAY);
    /** Tốc độ trung bình dùng để ước tính thời gian đến (km/h) - xe máy trong nội thành. */
    private static final double AVG_SPEED_KMH = 25.0;
    /** Bỏ qua tin gửi quá dày (tối đa 1 tin/giây cho mỗi yêu cầu). */
    private static final long MIN_INTERVAL_MS = 1000;

    private final RepairRequestRepository requests;
    private final SimpMessagingTemplate broker;
    /** Vị trí gần nhất của từng yêu cầu (lưu trong RAM, mất khi restart - đủ cho theo dõi tức thời). */
    private final Map<Long, TrackingEvent> last = new ConcurrentHashMap<>();

    public TrackingService(RepairRequestRepository requests, SimpMessagingTemplate broker) {
        this.requests = requests;
        this.broker = broker;
    }

    /** Thợ gửi vị trí -> kiểm tra quyền -> tính khoảng cách/ETA -> phát cho khách. */
    @Transactional
    public void handleLocation(Long requestId, Long userId, LocationMessage m) {
        if (m == null || m.lat() == null || m.lng() == null
                || m.lat() < -90 || m.lat() > 90 || m.lng() < -180 || m.lng() > 180) {
            throw new BadRequestException("Invalid location");
        }
        RepairRequest r = requests.findById(requestId).orElseThrow(() -> new NotFoundException("Repair request not found"));
        if (r.getWorker() == null || !r.getWorker().getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not assigned to this request");
        }
        if (!TRACKABLE.contains(r.getStatus())) {
            throw new BadRequestException("Request is not in a trackable state");
        }

        long now = System.currentTimeMillis();
        TrackingEvent prev = last.get(requestId);
        if (prev != null && now - prev.updatedAt() < MIN_INTERVAL_MS) return;

        // Thợ bắt đầu chia sẻ vị trí = bắt đầu di chuyển
        if (r.getStatus() == RepairStatus.MATCHED) r.setStatus(RepairStatus.ON_THE_WAY);

        Long distance = null;
        Integer eta = null;
        if (r.getLat() != null && r.getLng() != null) {
            double meters = haversineMeters(m.lat(), m.lng(), r.getLat().doubleValue(), r.getLng().doubleValue());
            distance = Math.round(meters);
            eta = meters < 50 ? 0 : (int) Math.ceil(meters / 1000.0 / AVG_SPEED_KMH * 60.0);
        }

        TrackingEvent ev = new TrackingEvent(requestId, r.getStatus().name(), m.lat(), m.lng(), m.heading(), distance, eta, now);
        last.put(requestId, ev);
        broker.convertAndSend("/topic/requests/" + requestId + "/location", ev);
    }

    /** Khách (hoặc thợ được ghép) xem ảnh chụp theo dõi hiện tại. */
    @Transactional(readOnly = true)
    public TrackingSnapshot snapshot(Long requestId, Long userId) {
        if (!canView(userId, false, requestId)) throw new NotFoundException("Repair request not found");
        RepairRequest r = requests.findById(requestId).orElseThrow(() -> new NotFoundException("Repair request not found"));

        TrackingSnapshot.WorkerInfo worker = null;
        if (r.getWorker() != null) {
            User u = r.getWorker().getUser();
            worker = new TrackingSnapshot.WorkerInfo(u.getFullName(), u.getPhoneNumber(), u.getAvatarUrl(), r.getWorker().getAverageRating());
        }
        TrackingEvent loc = null;
        if (TRACKABLE.contains(r.getStatus())) {
            loc = last.get(requestId);
        } else {
            last.remove(requestId);
        }
        return new TrackingSnapshot(r.getId(), r.getRequestCode(), r.getStatus().name(),
                new TrackingSnapshot.Destination(toDouble(r.getLat()), toDouble(r.getLng()), r.getAddressText()),
                worker, loc);
    }

    /** Các đơn thợ đang thực hiện (để thợ chọn đơn chia sẻ vị trí). */
    @Transactional(readOnly = true)
    public List<ActiveJob> activeJobs(Long userId) {
        return requests.findActiveByWorkerUser(userId, TRACKABLE).stream()
                .map(r -> new ActiveJob(r.getId(), r.getRequestCode(), r.getStatus().name(), r.getCategory().getName(),
                        r.getDescription(), r.getAddressText(), toDouble(r.getLat()), toDouble(r.getLng())))
                .toList();
    }

    public boolean canView(Long userId, boolean admin, Long requestId) {
        if (admin) return requests.existsById(requestId);
        return requests.existsByIdAndCustomerId(requestId, userId) || requests.existsByIdAndWorker_User_Id(requestId, userId);
    }

    private static Double toDouble(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }

    private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
