package com.thonha.backend.dto.response;

import com.thonha.backend.enums.CategoryStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private CategoryStatus status;

    public static CategoryResponse from(com.thonha.backend.entity.ServiceCategory c) {
        if (c == null) return null;
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .icon(c.getIcon())
                .status(c.getStatus())
                .build();
    }
}