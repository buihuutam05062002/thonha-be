package com.thonha.backend.entity;


import com.thonha.backend.enums.ComplaintReason;
import com.thonha.backend.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @ToString.Exclude
    private RepairRequest request;

    // Người gửi khiếu nại (khách hoặc thợ). Cần thêm cột này vào ERD
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "complainant_id", nullable = false)
    @ToString.Exclude
    private User complainant;

    // Admin xử lý, null khi chưa có ai nhận
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_id")
    @ToString.Exclude
    private User handledBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintReason reason;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.PENDING;

    @Column(columnDefinition = "text")
    private String resolution;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<ComplaintImage> images = new ArrayList<>();

    public void addImage(String url) {
        images.add(ComplaintImage.builder().complaint(this).url(url).build());
    }
}
