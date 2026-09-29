package com.thonha.backend.entity;


import lombok.Getter;

@Getter
public enum ApprovalStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Bị từ chối");

    private final String description;
    ApprovalStatus(String description) { this.description = description; }
}