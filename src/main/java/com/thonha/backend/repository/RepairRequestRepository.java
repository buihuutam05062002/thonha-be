package com.thonha.backend.repository;

import com.thonha.backend.entity.RepairRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long> {
    List<RepairRequest> findByCustomerIdOrderByCreatedAtDesc(Long userId);

    Optional<RepairRequest> findByIdAndCustomerId(Long id, Long userId);

    List<RepairRequest> findByWorkerIdOrderByCreatedAtDesc(Long workerId);

    boolean existsByIdAndCustomerId(Long id, Long customerId);

    boolean existsByIdAndWorker_User_Id(Long id, Long userId);

    List<RepairRequest> findByWorker_User_IdAndStatusInOrderByCreatedAtDesc(Long userId, Collection<RepairRequest.RepairStatus> statuses);
}