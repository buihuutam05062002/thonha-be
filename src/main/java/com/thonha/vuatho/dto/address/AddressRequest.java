package com.thonha.vuatho.dto.address;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AddressRequest(@Size(max = 100) String label, @NotBlank @Size(max = 500) String fullAddress,
                             @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
                             @DecimalMin("-180") @DecimalMax("180") BigDecimal lng, boolean defaultAddress) {
}
