package com.thonha.backend.repository;

import com.thonha.backend.entity.*;
import com.thonha.backend.enums.ApprovalStatus;
import com.thonha.backend.enums.AvailabilityStatus;

import java.util.*;
import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {
    Optional<WorkerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    // KHÔNG đưa "documents" và "specialties" (2 bag) vào EntityGraph: gây MultipleBagFetchException
    // và làm Hibernate phân trang trong bộ nhớ. Hai collection này được nạp theo lô (@BatchSize).
    @EntityGraph(attributePaths = {"user", "reviewedBy", "bankAccount"})
    Optional<WorkerProfile> findWithDetailById(Long id);

    @EntityGraph(attributePaths = {"user", "reviewedBy"})
    @Query(value = "select w from WorkerProfile w join w.user u " +
            "where (:status is null or w.approvalStatus = :status) " +
            "and (:keyword is null or lower(u.fullName) like lower(concat('%', :keyword, '%')) " +
            "     or lower(u.email) like lower(concat('%', :keyword, '%')) " +
            "     or u.phoneNumber like concat('%', :keyword, '%')) " +
            "and (:city is null or lower(w.provinceCity) like lower(concat('%', :city, '%')))",
            countQuery = "select count(w) from WorkerProfile w join w.user u " +
            "where (:status is null or w.approvalStatus = :status) " +
            "and (:keyword is null or lower(u.fullName) like lower(concat('%', :keyword, '%')) " +
            "     or lower(u.email) like lower(concat('%', :keyword, '%')) " +
            "     or u.phoneNumber like concat('%', :keyword, '%')) " +
            "and (:city is null or lower(w.provinceCity) like lower(concat('%', :city, '%')))")
    Page<WorkerProfile> search(@Param("status") ApprovalStatus status, @Param("keyword") String keyword,
                               @Param("city") String city, Pageable pageable);

    long countByApprovalStatus(ApprovalStatus status);

    // Tìm thợ khả dụng để matching
    @Query("select w from WorkerProfile w " +
            "join w.user u " +
            "join w.specialties s " +
            "where w.approvalStatus = :approvalStatus " +
            "and w.availabilityStatus = :availabilityStatus " +
            "and u.status = com.thonha.backend.enums.UserStatus.ACTIVE " +
            "and s.serviceCategory.id = :categoryId " +
            "and w.ongoingJobsCount < 3 " +
            "and (:city is null or lower(w.provinceCity) like lower(concat('%',:city,'%'))) ")
    List<WorkerProfile> findAvailableWorkers(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("availabilityStatus") AvailabilityStatus availabilityStatus,
            @Param("categoryId") Long categoryId,
            @Param("city") String city);
}
