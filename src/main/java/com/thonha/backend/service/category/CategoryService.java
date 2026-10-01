package com.thonha.backend.service.category;

import com.thonha.backend.dto.category.CategoryResponse;
import com.thonha.backend.entity.CategoryStatus;
import com.thonha.backend.repository.category.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final ServiceCategoryRepository serviceCategoryRepository;
    @Transactional(readOnly = true)
    public List<CategoryResponse> getListServiceCategory(){
        return serviceCategoryRepository.findByStatus(CategoryStatus.ACTIVE).stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
