package com.thonha.backend.dto.request;

import com.thonha.backend.entity.ApprovalStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class WorkerProfileSearchRequest {
    private String keyword;
    private ApprovalStatus status;   // null = tất cả
    private String city;
}