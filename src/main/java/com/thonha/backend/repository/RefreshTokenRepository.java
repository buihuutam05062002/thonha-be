package com.thonha.backend.repository;

import com.thonha.backend.entity.RefreshToken;

import java.util.*;

import com.thonha.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String hash);
}
