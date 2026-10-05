package com.thonha.backend.dto.response;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private String status;
    private Set<String> roles;

    public static UserResponse from(com.thonha.backend.entity.User user) {
        if (user == null) return null;
        Set<String> roles = user.getRoles() != null 
            ? user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet())
            : Set.of();
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .roles(roles)
                .build();
    }
}