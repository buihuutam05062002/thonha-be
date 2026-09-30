package com.thonha.backend.repository.category;

import com.thonha.backend.entity.CategoryStatus;
import com.thonha.backend.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    List<ServiceCategory> findByStatus(CategoryStatus status);
    List<ServiceCategory> findByIdInAndStatus(Collection<Long> ids, CategoryStatus status);
}
