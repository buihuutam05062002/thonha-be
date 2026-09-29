    package com.thonha.backend.dto.response;

    import lombok.*;
    import lombok.experimental.FieldDefaults;

    import java.time.LocalDateTime;
    import java.util.Set;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Builder
    public class UserResponse {
        Long id;
        String name;
        String email;
        String phoneNumber;
        Set<String> roles;
        String avatar;
        LocalDateTime createdAt;
        String userStatus;

    }
