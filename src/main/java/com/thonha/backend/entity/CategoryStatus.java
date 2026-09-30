package com.thonha.backend.entity;

public enum CategoryStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động");

    private String description ;

    CategoryStatus(String description) {
        this.description = description;
    }
}
