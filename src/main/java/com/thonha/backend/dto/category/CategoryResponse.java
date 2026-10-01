package com.thonha.backend.dto.category;

import com.thonha.backend.entity.ServiceCategory;

public record CategoryResponse(Long id, String name, String desciption, String icon) {
    public static CategoryResponse from(ServiceCategory category){
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(), category.getIcon());
    }
}
