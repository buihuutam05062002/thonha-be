package com.thonha.vuatho.dto.worker;

import jakarta.validation.constraints.*;

import java.util.List;

public record RegisterWorkerProfileRequest(@NotBlank @Size(max = 100) String provinceCity,
                                           @NotBlank @Size(max = 255) String operatingArea,
                                           @NotNull @Min(0) @Max(60) Integer experienceYears,
                                           @NotEmpty List<@NotNull Long> categoryIds, @AssertTrue boolean agreePolicy) {
}
