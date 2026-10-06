package com.thonha.backend.service;

import com.thonha.backend.dto.category.CategoryResponse;
import com.thonha.backend.entity.CategoryStatus;
import com.thonha.backend.repository.ServiceCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CategoryService {
    private final ServiceCategoryRepository repo;

    public CategoryService(ServiceCategoryRepository r) {
        repo = r;
    }

    public List<CategoryResponse> list() {
        return repo.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE).stream().map(CategoryResponse::from).toList();
    }
}
