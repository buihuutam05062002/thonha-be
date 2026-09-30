package com.thonha.backend.dto.worker;

import jakarta.validation.constraints.*;

import java.util.List;

public record RegisterWorkerProfileRequest(
    @NotBlank(message = "Vui lòng chọn tỉnh/thành phố")
    @Size(max = 100, message = "Tỉnh/thành phố tối đa 100 ký tự")
    String provinceCity,
    
    @NotBlank(message = "Vui lòng chọn khu vực làm việc")
    @Size(max = 255, message = "Khu vực tối đa 255 ký tự")
    String operatingArea,
    
    @NotNull(message = "Vui lòng nhập số năm kinh nghiệm")
    @Min(value = 0, message = "Kinh nghiệm không được âm")
    @Max(value = 60, message = "Kinh nghiệm tối đa 60 năm")
    Integer experienceYears,
    
    @NotEmpty(message = "Vui lòng chọn ít nhất 1 chuyên môn")
    List<@NotNull(message = "Mã chuyên môn không hợp lệ") Long> categoryIds,
    
    @AssertTrue(message = "Bạn cần đồng ý chính sách bảo vệ dữ liệu cá nhân")
    boolean agreePolicy
){}
