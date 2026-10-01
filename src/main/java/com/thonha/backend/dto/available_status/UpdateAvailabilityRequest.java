package com.thonha.backend.dto.available_status;

import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(@NotNull(message = "Vui lòng gửi giá trị trạng thái") Boolean available) {
}
