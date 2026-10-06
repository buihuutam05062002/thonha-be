package com.thonha.backend.repository;

import com.thonha.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    interface RatingStats {
        Double getAvg();

        Long getCnt();
    }

    boolean existsByRequestId(Long requestId);

    Optional<Review> findByRequestId(Long requestId);

    Page<Review> findByWorker_User_IdOrderByCreatedAtDesc(Long workerUserId, Pageable pageable);

    @Query("select avg(r.starRating) as avg, count(r) as cnt from Review r where r.worker.id = :workerId")
    RatingStats statsForWorker(@Param("workerId") Long workerId);

    @Query("select avg(r.starRating) as avg, count(r) as cnt from Review r where r.worker.user.id = :userId")
    RatingStats statsForWorkerUser(@Param("userId") Long userId);
}