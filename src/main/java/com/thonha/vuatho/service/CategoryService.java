package com.thonha.vuatho.service;

import com.thonha.vuatho.dto.category.CategoryResponse;
import com.thonha.vuatho.entity.CategoryStatus;
import com.thonha.vuatho.repository.ServiceCategoryRepository;
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
