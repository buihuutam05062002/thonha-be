package com.thonha.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessMs, refreshMs;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.access-minutes:30}") long am, @Value("${app.jwt.refresh-days:7}") long rd) {
        if (secret == null || secret.isBlank()) throw new IllegalStateException("JWT_SECRET is required");
        byte[] raw = Base64.getDecoder().decode(secret);
        if (raw.length < 32) throw new IllegalArgumentException("JWT_SECRET must decode to at least 32 bytes");
        key = Keys.hmacShaKeyFor(raw);
        accessMs = am * 60_000L;
        refreshMs = rd * 86_400_000L;
    }

    public String accessToken(Long id, Collection<String> roles) {
        return create(id, roles, "access", accessMs);
    }

    public String refreshToken(Long id) {
        return create(id, List.of(), "refresh", refreshMs);
    }

    private String create(Long id, Collection<String> roles, String type, long ttl) {
        var b = Jwts.builder().subject(id.toString()).claim("type", type).claim("roles", roles).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + ttl));
        return b.signWith(key).compact();
    }

    public Claims parse(String t) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(t).getPayload();
    }

    public long accessExpiresIn() {
        return accessMs / 1000;
    }

    public boolean isAccess(String t) {
        return "access".equals(parse(t).get("type", String.class));
    }
}
