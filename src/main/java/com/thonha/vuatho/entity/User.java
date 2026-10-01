package com.thonha.vuatho.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(length = 100)
    private String username;
    @Column(unique = true, length = 150)
    private String email;
    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;
    @Column(nullable = false, length = 255)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    void pre() {
        var n = LocalDateTime.now();
        createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    void upd() {
        updatedAt = LocalDateTime.now();
    }

    public boolean hasRole(String role) {
        return roles.stream().anyMatch(r -> role.equals(r.getName()));
    }
}
