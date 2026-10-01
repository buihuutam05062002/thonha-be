package com.thonha.vuatho.repository;

import com.thonha.vuatho.entity.RepairRequest;

import java.util.*;

import org.springframework.data.jpa.repository.*;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long> {
    List<RepairRequest> findByCustomerIdOrderByCreatedAtDesc(Long userId);

    Optional<RepairRequest> findByIdAndCustomerId(Long id, Long userId);

    List<RepairRequest> findByWorkerIdOrderByCreatedAtDesc(Long workerId);
}
