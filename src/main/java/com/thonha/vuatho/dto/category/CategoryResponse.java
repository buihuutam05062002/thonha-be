package com.thonha.vuatho.dto.category;

import com.thonha.vuatho.entity.ServiceCategory;

public record CategoryResponse(Long id, String name, String description, String icon) {
    public static CategoryResponse from(ServiceCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getIcon());
    }
}
