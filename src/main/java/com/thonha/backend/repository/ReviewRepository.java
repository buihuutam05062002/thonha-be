package com.thonha.backend.repository;

import com.thonha.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    long countByWorkerId(Long workerId);
}
