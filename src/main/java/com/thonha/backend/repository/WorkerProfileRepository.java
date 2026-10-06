package com.thonha.backend.repository;

import com.thonha.backend.entity.*;

import java.util.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {
    Optional<WorkerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "documents", "specialties", "specialties.serviceCategory", "reviewedBy"})
    Optional<WorkerProfile> findWithDetailById(Long id);

    @Query("select w from WorkerProfile w join w.user u where (:status is null or w.approvalStatus=:status) and (:keyword is null or lower(u.fullName) like lower(concat('%',:keyword,'%')) or lower(u.email) like lower(concat('%',:keyword,'%'))) and (:city is null or lower(w.provinceCity) like lower(concat('%',:city,'%')))")
    Page<WorkerProfile> search(@Param("status") ApprovalStatus status, @Param("keyword") String keyword, @Param("city") String city, Pageable pageable);
}
