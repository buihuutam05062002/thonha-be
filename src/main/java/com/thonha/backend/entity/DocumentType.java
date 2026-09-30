package com.thonha.backend.entity;

public enum DocumentType {
    CCCD_FRONT("CCCD mặt trước"),
    CCCD_BACK("CCCD mặt sau"),
    CERTIFICATE("Chứng chỉ"),
    DEGREE("Bằng cấp");

    private String description ;

    DocumentType(String description) {
        this.description = description;
    }
}
