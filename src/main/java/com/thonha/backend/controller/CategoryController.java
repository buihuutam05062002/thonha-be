package com.thonha.backend.controller;

import com.thonha.backend.dto.category.CategoryResponse;
import com.thonha.backend.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/service-categories")
public class CategoryController {
    private final CategoryService s;

    public CategoryController(CategoryService s) {
        this.s = s;
    }

    @GetMapping
    List<CategoryResponse> list() {
        return s.list();
    }
}
