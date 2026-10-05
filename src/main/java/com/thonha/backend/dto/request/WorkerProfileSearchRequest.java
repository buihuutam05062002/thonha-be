package com.thonha.backend.dto.request;

import com.thonha.backend.enums.ApprovalStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerProfileSearchRequest {
    private String keyword;
    private ApprovalStatus status;
    private String city;
}