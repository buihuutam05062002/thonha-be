package com.thonha.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String account;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}