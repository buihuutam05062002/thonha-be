package com.thonha.backend.entity;

import jakarta.persistence.*;

import lombok.*;


import java.math.BigDecimal;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "worker_profile")
@Getter
@Setter
@NoArgsConstructor
public class WorkerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "user_id", nullable = false,unique = true)
    private User user;
    
    @Column(nullable = false, length = 100)
    private String provinceCity;
    
    @Column(nullable = false)
    private String operatingArea;
    
    @Column(nullable = false)
    private Integer experienceYears;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.OFFLINE;
    
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal acceptanceRate = new BigDecimal("100.00");
    
    @Column(nullable = false)
    private Integer ongoingJobsCount = 0;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;




    
    @OneToMany(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WorkerSpecialty> specialties = new HashSet<>();

    public void addSpecialty(ServiceCategory category){
        WorkerSpecialty specialty = new WorkerSpecialty();
        specialty.setWorkerProfile(this);
        specialty.setServiceCategory(category);
        this.specialties.add(specialty);
    }

    @OneToMany(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WorkerDocument> documents = new HashSet<>();

    public void addDocument(DocumentType type, String fileUrl) {
        WorkerDocument doc = new WorkerDocument();
        doc.setWorkerProfile(this);
        doc.setType(type);
        doc.setFileUrl(fileUrl);
        this.documents.add(doc);
    }
}
