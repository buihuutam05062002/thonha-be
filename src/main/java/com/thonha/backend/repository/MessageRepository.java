package com.thonha.backend.repository;

import com.thonha.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRequest_IdOrderBySentAtAsc(Long requestId);

    /** Trang mới nhất của cuộc trò chuyện (id giảm dần, service sẽ đảo lại thành cũ -> mới). */
    List<Message> findByRequest_IdOrderByIdDesc(Long requestId, Pageable pageable);

    /** Trang cũ hơn: các tin có id nhỏ hơn tin cũ nhất đang hiển thị (phân trang theo con trỏ). */
    List<Message> findByRequest_IdAndIdLessThanOrderByIdDesc(Long requestId, Long beforeId, Pageable pageable);

    /** Khi 1 phía mở màn chat ra xem -> đánh dấu đã đọc toàn bộ tin của phía kia. */
    @Modifying
    @Query("update Message m set m.seen = true where m.request.id = :requestId and m.sender.id <> :viewerId and m.seen = false")
    int markSeen(@Param("requestId") Long requestId, @Param("viewerId") Long viewerId);

    long countByRequest_IdAndSender_IdNotAndSeenFalse(Long requestId, Long viewerId);
}