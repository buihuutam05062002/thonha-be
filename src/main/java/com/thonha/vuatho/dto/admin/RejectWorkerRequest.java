package com.thonha.vuatho.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectWorkerRequest(@NotBlank @Size(max = 500) String reason) {
}
