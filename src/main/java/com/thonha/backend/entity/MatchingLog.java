package com.thonha.backend.entity;


import com.thonha.backend.enums.MatchingResult;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "matching_log", uniqueConstraints = @UniqueConstraint(name = "uk_matching_request_worker", columnNames = {"request_id", "worker_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class MatchingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @ToString.Exclude
    private RepairRequest request;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    @ToString.Exclude
    private WorkerProfile worker;

    @Column(name = "total_score", precision = 7, scale = 4)
    private BigDecimal totalScore;

    @Column(name = "distance_score", precision = 7, scale = 4)
    private BigDecimal distanceScore;

    @Column(name = "rating_score", precision = 7, scale = 4)
    private BigDecimal ratingScore;

    @Column(name = "acceptance_rate_score", precision = 7, scale = 4)
    private BigDecimal acceptanceRateScore;

    @Column(name = "workload_score", precision = 7, scale = 4)
    private BigDecimal workloadScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MatchingResult result = MatchingResult.PENDING;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(columnDefinition = "text")
    private String note;
}
