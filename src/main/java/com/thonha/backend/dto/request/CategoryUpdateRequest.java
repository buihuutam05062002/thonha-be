package com.thonha.backend.dto.request;

import com.thonha.backend.enums.CategoryStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpdateRequest {
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @Size(max = 255, message = "Icon tối đa 255 ký tự")
    private String icon;

    private CategoryStatus status;
}