package com.thonha.vuatho.service;

import com.thonha.vuatho.dto.*;
import com.thonha.vuatho.dto.auth.*;
import com.thonha.vuatho.entity.*;
import com.thonha.vuatho.exception.*;
import com.thonha.vuatho.repository.*;
import com.thonha.vuatho.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final long refreshDays;

    public AuthService(UserRepository u, RoleRepository r, RefreshTokenRepository t, PasswordEncoder e, JwtService j, @Value("${app.jwt.refresh-days:7}") long refreshDays) {
        users = u;
        roles = r;
        tokens = t;
        encoder = e;
        jwt = j;
        this.refreshDays = refreshDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = blank(r.email()), phone = blank(r.phoneNumber());
        if (email == null && phone == null) throw new BadRequestException("Email or phone is required");
        if (email != null && users.existsByEmailIgnoreCase(email))
            throw new BadRequestException("Email is already in use");
        if (phone != null && users.existsByPhoneNumber(phone)) throw new BadRequestException("Phone is already in use");
        User u = new User();
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPhoneNumber(phone);
        u.setPassword(encoder.encode(r.password()));
        u.setStatus(UserStatus.ACTIVE);
        u.getRoles().add(role(Role.CUSTOMER));
        users.save(u);
        return issue(u);
    }

    @Transactional
    public AuthResponse login(LoginRequest r) {
        String x = r.account().trim();
        User u = x.contains("@") ? users.findByEmailIgnoreCase(x).orElse(null) : users.findByPhoneNumber(x).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPassword()))
            throw new UnauthorizedException("Invalid account or password");
        if (u.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Account is not active");
        return issue(u);
    }

    @Transactional
    public AuthResponse refresh(String raw) {
        RefreshToken rt = tokens.findByTokenHash(hash(raw)).orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (rt.getRevokedAt() != null || rt.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new UnauthorizedException("Refresh token expired");
        User u = rt.getUser();
        if (u.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Account is not active");
        rt.setRevokedAt(LocalDateTime.now());
        return issue(u);
    }

    @Transactional
    public void logout(String raw) {
        tokens.findByTokenHash(hash(raw)).ifPresent(t -> t.setRevokedAt(LocalDateTime.now()));
    }

    public UserResponse me(Long id) {
        return UserResponse.from(users.findById(id).orElseThrow(() -> new UnauthorizedException("Account not found")));
    }

    @Transactional
    public UserResponse update(Long id, String fullName, String avatarUrl) {
        User u = users.findById(id).orElseThrow(() -> new UnauthorizedException("Account not found"));
        u.setFullName(fullName.trim());
        u.setAvatarUrl(blank(avatarUrl));
        return UserResponse.from(u);
    }

    private Role role(String n) {
        return roles.findByName(n).orElseThrow(() -> new IllegalStateException("Role not configured: " + n));
    }

    private AuthResponse issue(User u) {
        String a = jwt.accessToken(u.getId(), u.getRoles().stream().map(Role::getName).toList()), r = jwt.refreshToken(u.getId());
        RefreshToken rt = new RefreshToken();
        rt.setUser(u);
        rt.setTokenHash(hash(r));
        rt.setExpiresAt(LocalDateTime.now().plusDays(refreshDays));
        tokens.save(rt);
        return new AuthResponse(a, r, jwt.accessExpiresIn(), UserResponse.from(u));
    }

    private static String blank(String x) {
        return x == null || x.isBlank() ? null : x.trim();
    }

    private static String hash(String x) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
