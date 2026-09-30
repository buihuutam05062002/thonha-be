package com.thonha.backend.repository.worker;

import com.thonha.backend.entity.WorkerProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {
    boolean existsByUserId(Long userId);
    Optional<WorkerProfile> findByUserId(Long userId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkerProfile w where w.user.id = :userId")
    Optional<WorkerProfile> findByUserIdForUpdate(@Param("userId")Long userId);
}
