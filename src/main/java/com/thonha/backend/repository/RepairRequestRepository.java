package com.thonha.backend.repository;

import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.enums.RepairStatus;

import java.util.*;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long> {
    List<RepairRequest> findByCustomerIdOrderByCreatedAtDesc(Long userId);

    Optional<RepairRequest> findByIdAndCustomerId(Long id, Long userId);

    List<RepairRequest> findByWorkerIdOrderByCreatedAtDesc(Long workerId);

    @Query("select r from RepairRequest r join fetch r.category join fetch r.customer where r.id = :id")
    Optional<RepairRequest> findByIdWithDetails(Long id);


    @Query("select r from RepairRequest r where r.customer.id = :userId and r.status in :statuses order by r.createdAt desc")
    List<RepairRequest> findByCustomerIdAndStatusIn(Long userId, List<RepairStatus> statuses);

    @Query("select r from RepairRequest r where r.worker.id = :workerId and r.status in :statuses order by r.createdAt desc")
    List<RepairRequest> findByWorkerIdAndStatusIn(Long workerId, List<RepairStatus> statuses);

    @Query("select r from RepairRequest r where r.status = :status")
    List<RepairRequest> findByStatus(RepairStatus status);

    @Query("select r from RepairRequest r where r.status = :status")
    Page<RepairRequest> findByStatus(RepairStatus status, Pageable pageable);
}