package com.thonha.backend.entity;

import lombok.Getter;

public enum ApprovalStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECT("Từ chối");

    @Getter
    private final String description;

    ApprovalStatus(String description) {
        this.description = description;
    }
}

