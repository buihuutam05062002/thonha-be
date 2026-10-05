package com.thonha.backend.dto.request;

import com.thonha.backend.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {
    private String keyword;
    private UserStatus userStatus;
    private String role;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
}