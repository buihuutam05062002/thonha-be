package com.thonha.backend.service;

import com.thonha.backend.enums.AvailabilityStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerLocationService {

    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;
    private static final String WORKER_LOCATION_KEY = "worker:locations";
    private static final String WORKER_STATUS_KEY = "worker:status";
    private static final String WORKER_LAST_UPDATE_KEY = "worker:last_update";

    /**
     * Cập nhật vị trí thực tế của thợ (gọi từ app worker mỗi 10-30s)
     */
    public void updateWorkerLocation(Long workerId, Double lat, Double lng, AvailabilityStatus status) {
        String workerKey = "worker:" + workerId;
        
        // Cập nhật vị trí Geo
        redisTemplate.opsForGeo().add(WORKER_LOCATION_KEY, new Point(lng, lat), "worker:" + workerId);
        
        // Cập nhật status
        redisTemplate.opsForHash().put(WORKER_STATUS_KEY, "worker:" + workerId, status.name());
        
        // Timestamp cập nhật cuối
        redisTemplate.opsForHash().put(WORKER_LAST_UPDATE_KEY, "worker:" + workerId, String.valueOf(System.currentTimeMillis()));
        
        log.debug("Updated location for worker {}: lat={}, lng={}, status={}", workerId, lat, lng, status);
    }

    /**
     * Lấy vị trí hiện tại của thợ
     */
    public WorkerLocation getWorkerLocation(Long workerId) {
        Point point = redisTemplate.opsForGeo().position(WORKER_LOCATION_KEY, "worker:" + workerId).get(0);
        String status = (String) redisTemplate.opsForHash().get(WORKER_STATUS_KEY, "worker:" + workerId);
        String lastUpdate = (String) redisTemplate.opsForHash().get(WORKER_LAST_UPDATE_KEY, "worker:" + workerId);
        
        if (point == null) return null;
        
        return WorkerLocation.builder()
                .workerId(workerId)
                .lat(point.getY())
                .lng(point.getX())
                .status(AvailabilityStatus.valueOf(status))
                .lastUpdate(lastUpdate != null ? Long.parseLong(lastUpdate) : null)
                .build();
    }

    /**
     * Tìm thợ gần nhất trong bán kính (km) - dùng GEOSEARCH Redis
     */
    public List<NearbyWorker> findNearbyWorkers(double lat, double lng, double radiusKm, String categoryId) {
        Point center = new Point(lng, lat);
        org.springframework.data.geo.Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        
        org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs args = 
                org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeCoordinates()
                .includeDistance()
                .sortAscending()
                .limit(50); // Giới hạn 50 thợ gần nhất
        
        org.springframework.data.geo.GeoResults<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .radius(WORKER_LOCATION_KEY, new org.springframework.data.geo.Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS)), 
                        org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeCoordinates().includeDistance().sortAscending().limit(50));
        
        if (results == null) return List.of();
        
        return results.getContent().stream()
                .map(result -> {
                    String workerKey = result.getContent().getName(); // "worker:123"
                    Long workerId = Long.parseLong(workerKey.replace("worker:", ""));
                    Point point = result.getContent().getPoint();
                    double distanceKm = result.getDistance().getValue();
                    
                    // Lấy status từ hash
                    String statusStr = (String) redisTemplate.opsForHash().get(WORKER_STATUS_KEY, "worker:" + workerId);
                    AvailabilityStatus status = statusStr != null ? AvailabilityStatus.valueOf(statusStr) : AvailabilityStatus.OFFLINE;
                    
                    return NearbyWorker.builder()
                            .workerId(workerId)
                            .lat(point.getY())
                            .lng(point.getX())
                            .distanceKm(result.getDistance().getValue())
                            .status(AvailabilityStatus.valueOf(statusStr))
                            .build();
                })
                .filter(w -> w.getStatus() == AvailabilityStatus.READY) // Chỉ lấy thợ READY
                .collect(Collectors.toList());
    }

    /**
     * Xóa vị trí khi thợ offline/logout
     */
    public void removeWorkerLocation(Long workerId) {
        redisTemplate.opsForGeo().remove(WORKER_LOCATION_KEY, "worker:" + workerId);
        redisTemplate.opsForHash().delete(WORKER_STATUS_KEY, "worker:" + workerId);
        redisTemplate.opsForHash().delete(WORKER_LAST_UPDATE_KEY, "worker:" + workerId);
        log.info("Removed location for worker {}", workerId);
    }

    /**
     * Kiểm tra thợ có online không (cập nhật < 5 phút)
     */
    public boolean isWorkerOnline(Long workerId) {
        String lastUpdate = (String) redisTemplate.opsForHash().get(WORKER_LAST_UPDATE_KEY, "worker:" + workerId);
        if (lastUpdate == null) return false;
        long lastUpdateTime = Long.parseLong(lastUpdate);
        return (System.currentTimeMillis() - lastUpdateTime) < 5 * 60 * 1000; // 5 phút
    }

    /**
     * Lấy danh sách ID thợ đang online theo category
     */
    public List<Long> getOnlineWorkerIdsByCategory(String categoryId) {
        // TODO: Implement - cần index thêm categoryId vào Redis hoặc query từ DB
        // Tạm thời return empty, sẽ implement sau khi có category index
        return List.of();
    }

    // Inner DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WorkerLocation {
        private Long workerId;
        private Double lat;
        private Double lng;
        private AvailabilityStatus status;
        private Long lastUpdate;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NearbyWorker {
        private Long workerId;
        private Double lat;
        private Double lng;
        private double distanceKm;
        private AvailabilityStatus status;
    }
}