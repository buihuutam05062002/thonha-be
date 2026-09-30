package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "worker_document")
@Getter
@Setter
@NoArgsConstructor
public class WorkerDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_profile_id", nullable = false)
    private WorkerProfile workerProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentType type;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
