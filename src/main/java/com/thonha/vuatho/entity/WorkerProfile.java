package com.thonha.vuatho.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "worker_profile")
@Getter
@Setter
@NoArgsConstructor
public class WorkerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    User user;
    @Column(name = "province_city", nullable = false, length = 100)
    String provinceCity;
    @Column(name = "operating_area", nullable = false, length = 255)
    String operatingArea;
    @Column(name = "experience_years", nullable = false)
    Integer experienceYears;
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    AvailabilityStatus availabilityStatus = AvailabilityStatus.OFFLINE;
    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    BigDecimal averageRating = BigDecimal.ZERO;
    @Column(name = "acceptance_rate", nullable = false, precision = 5, scale = 2)
    BigDecimal acceptanceRate = new BigDecimal("100.00");
    @Column(name = "ongoing_jobs_count", nullable = false)
    Integer ongoingJobsCount = 0;
    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    User reviewedBy;
    @Column(name = "reject_reason", length = 500)
    String rejectReason;
    @OneToMany(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<WorkerDocument> documents = new HashSet<>();
    @OneToMany(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<WorkerSpecialty> specialties = new HashSet<>();

    public void addDocument(DocumentType t, String url) {
        WorkerDocument d = new WorkerDocument();
        d.setWorkerProfile(this);
        d.setType(t);
        d.setFileUrl(url);
        documents.add(d);
    }

    public void addSpecialty(ServiceCategory c) {
        WorkerSpecialty s = new WorkerSpecialty();
        s.setWorkerProfile(this);
        s.setServiceCategory(c);
        specialties.add(s);
    }
}
