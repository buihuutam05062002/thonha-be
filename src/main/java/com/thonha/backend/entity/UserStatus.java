package com.thonha.backend.entity;

import lombok.Getter;
@Getter
public enum UserStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động"),
    LOCKED("Bị khóa"),
    DELETED("Đã xóa");

    private String description ;

    UserStatus(String description) {
        this.description = description;
    }
}
