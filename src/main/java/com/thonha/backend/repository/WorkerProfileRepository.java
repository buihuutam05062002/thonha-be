package com.thonha.backend.repository;

import com.thonha.backend.entity.ApprovalStatus;
import com.thonha.backend.entity.WorkerProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {

    @EntityGraph(attributePaths = {"user"})
    @Query("""
        SELECT w FROM WorkerProfile w
        WHERE (:status IS NULL OR w.approvalStatus = :status)
          AND (:keyword IS NULL
               OR LOWER(w.user.name)  LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(w.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR w.user.phoneNumber  LIKE CONCAT('%', :keyword, '%'))
          AND (:city IS NULL OR LOWER(w.residenceCity) LIKE LOWER(CONCAT('%', :city, '%')))
        """)
    Page<WorkerProfile> search(@Param("status") ApprovalStatus status,
                               @Param("keyword") String keyword,
                               @Param("city") String city,
                               Pageable pageable);

    @EntityGraph(attributePaths = {"user", "documents", "reviewedBy"})
    Optional<WorkerProfile> findWithDetailById(Long id);

    // Khóa dòng để 2 người duyệt cùng lúc không ghi đè nhau
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WorkerProfile w WHERE w.id = :id")
    Optional<WorkerProfile> findByIdForUpdate(@Param("id") Long id);
}