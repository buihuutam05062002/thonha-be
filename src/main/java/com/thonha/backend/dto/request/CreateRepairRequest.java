package com.thonha.backend.dto.request;

import com.thonha.backend.enums.PriorityLevel;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRepairRequest {
    @NotNull(message = "Danh mục dịch vụ là bắt buộc")
    private Long categoryId;

    private Long addressId;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    private String addressText;

    @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ")
    private Double lat;

    @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ")
    private Double lng;

    @NotNull(message = "Mức độ ưu tiên là bắt buộc")
    private PriorityLevel priorityLevel;

    private DesiredTime desiredTime;

    private LocalDateTime scheduledAt;

    public enum DesiredTime {
        ASAP, SCHEDULED
    }
}