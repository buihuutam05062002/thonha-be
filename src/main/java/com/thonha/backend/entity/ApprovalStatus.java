package com.thonha.backend.entity;

public enum ApprovalStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECT("Từ chối");

    private String description ;

    ApprovalStatus(String description) {
        this.description = description;
    }
}
