package com.thonha.vuatho.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "repair_request")
@Getter
@Setter
@NoArgsConstructor
public class RepairRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "request_code", nullable = false, unique = true, length = 50)
    String requestCode;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    User customer;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    ServiceCategory category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    Address address;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    WorkerProfile worker;
    @Column(nullable = false, columnDefinition = "text")
    String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 20)
    PriorityLevel priorityLevel = PriorityLevel.NORMAL;
    @Column(name = "address_text", length = 500)
    String addressText;
    @Column(precision = 10, scale = 7)
    BigDecimal lat;
    @Column(precision = 10, scale = 7)
    BigDecimal lng;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    RepairStatus status = RepairStatus.PENDING;
    @Column(name = "final_price", precision = 15, scale = 2)
    BigDecimal finalPrice;
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    List<RequestAttachment> attachments = new ArrayList<>();

    @PrePersist
    void pre() {
        var n = LocalDateTime.now();
        createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    void upd() {
        updatedAt = LocalDateTime.now();
    }

    public enum PriorityLevel {NORMAL, URGENT}

    public enum RepairStatus {PENDING, MATCHING, ASSIGNED, ON_THE_WAY, IN_PROGRESS, COMPLETED, CANCELLED}
}
