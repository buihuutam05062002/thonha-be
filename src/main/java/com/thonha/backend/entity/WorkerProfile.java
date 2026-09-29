package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "worker_profile")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    Users user;

    String serviceArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AvailabilityStatus availabilityStatus = AvailabilityStatus.OFFLINE;

    BigDecimal avgRating = BigDecimal.ZERO;
    BigDecimal acceptanceRate = BigDecimal.ZERO;
    Integer activeJobCount = 0;
    String residenceCity;
    Integer yearsOfExperience;

    // --- thông tin duyệt ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    Users reviewedBy;

    LocalDateTime reviewedAt;

    @Column(length = 500)
    String rejectReason;

    @OneToMany(mappedBy = "workerProfile", fetch = FetchType.LAZY)
    List<WorkerDocument> documents = new ArrayList<>();
}