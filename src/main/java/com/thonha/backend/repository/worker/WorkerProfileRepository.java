package com.thonha.backend.repository.worker;

import com.thonha.backend.entity.WorkerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {
    boolean existsByUserId(Long userId);
    Optional<WorkerProfile> findByUserId(Long userId);
}
