package com.thonha.backend.entity;

public enum AvailabilityStatus {
    READY("Sẵn sàng"),
    OFFLINE("Ngoại tuyến"),
    BUSY("Đang bận");

    private String description ;

    AvailabilityStatus(String description) {
        this.description = description;
    }
}
