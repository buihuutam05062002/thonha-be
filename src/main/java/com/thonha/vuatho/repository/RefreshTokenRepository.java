package com.thonha.vuatho.repository;

import com.thonha.vuatho.entity.RefreshToken;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String hash);
}
