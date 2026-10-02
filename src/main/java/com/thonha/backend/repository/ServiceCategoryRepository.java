package com.thonha.backend.repository;

import com.thonha.backend.entity.*;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    List<ServiceCategory> findByStatusOrderByIdAsc(CategoryStatus status);

    List<ServiceCategory> findByIdInAndStatus(Collection<Long> ids, CategoryStatus status);
}
