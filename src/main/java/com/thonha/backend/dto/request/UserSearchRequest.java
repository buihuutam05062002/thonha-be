package com.thonha.backend.dto.request;

import com.thonha.backend.entity.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserSearchRequest {
    String keyword;
    UserStatus userStatus;
    String role;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDateTime createdFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDateTime createdTo;

}
