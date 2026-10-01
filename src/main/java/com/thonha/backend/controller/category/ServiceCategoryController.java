package com.thonha.backend.controller.category;

import com.thonha.backend.dto.category.CategoryResponse;
import com.thonha.backend.service.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-category")
@RequiredArgsConstructor
public class ServiceCategoryController {
    private final CategoryService categoryService;
    
    @GetMapping
    public List<CategoryResponse> getListServiceCategory(){
        return categoryService.getListServiceCategory();
    }
}
