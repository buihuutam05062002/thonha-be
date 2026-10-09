package com.thonha.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterWorkerProfileRequest {
    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    @Size(max = 100, message = "Tỉnh/Thành phố tối đa 100 ký tự")
    private String provinceCity;

    @NotBlank(message = "Khu vực hoạt động không được để trống")
    @Size(max = 255, message = "Khu vực hoạt động tối đa 255 ký tự")
    private String operatingArea;

    @NotNull(message = "Số năm kinh nghiệm không được để trống")
    @Min(value = 0, message = "Kinh nghiệm không được âm")
    @Max(value = 60, message = "Kinh nghiệm tối đa 60 năm")
    private Integer experienceYears;

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 chuyên môn")
    @Size(max = 10, message = "Tối đa 10 chuyên môn")
    private List<@NotNull(message = "ID chuyên môn không hợp lệ") Long> categoryIds;

    @AssertTrue(message = "Bạn cần đồng ý chính sách bảo vệ dữ liệu cá nhân")
    private boolean agreePolicy;
}