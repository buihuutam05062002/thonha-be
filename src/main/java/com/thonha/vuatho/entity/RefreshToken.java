package com.thonha.vuatho.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    String tokenHash;
    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;
    @Column(name = "revoked_at")
    LocalDateTime revokedAt;
}
