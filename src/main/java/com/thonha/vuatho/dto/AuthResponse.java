package com.thonha.vuatho.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
}
