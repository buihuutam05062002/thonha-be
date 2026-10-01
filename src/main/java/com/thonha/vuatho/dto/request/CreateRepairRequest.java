package com.thonha.vuatho.dto.request;

import com.thonha.vuatho.entity.RepairRequest;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateRepairRequest(@NotNull Long categoryId, @NotBlank @Size(min = 10, max = 2000) String description,
                                  @NotNull RepairRequest.PriorityLevel priorityLevel, Long addressId,
                                  @NotBlank @Size(max = 500) String addressText,
                                  @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
                                  @DecimalMin("-180") @DecimalMax("180") BigDecimal lng,
                                  @NotNull DesiredTime desiredTime, LocalDateTime scheduledAt) {
    public enum DesiredTime {IMMEDIATE, SCHEDULED}
}
