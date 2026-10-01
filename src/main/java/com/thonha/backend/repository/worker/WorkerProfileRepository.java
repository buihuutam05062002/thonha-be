package com.thonha.backend.repository.worker;

import com.thonha.backend.entity.ApprovalStatus;
import com.thonha.backend.entity.WorkerProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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



    @EntityGraph(attributePaths = {"user"})
    @Query("""
        SELECT w FROM WorkerProfile w
        WHERE (:status IS NULL OR w.approvalStatus = :status)
          AND (:keyword IS NULL
               OR LOWER(w.user.fullName)  LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(w.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR w.user.phoneNumber  LIKE CONCAT('%', :keyword, '%'))
          AND (:city IS NULL OR LOWER(w.provinceCity) LIKE LOWER(CONCAT('%', :city, '%')))
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
