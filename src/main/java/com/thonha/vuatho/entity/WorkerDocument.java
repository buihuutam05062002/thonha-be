package com.thonha.vuatho.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "worker_document")
@Getter
@Setter
@NoArgsConstructor
public class WorkerDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_profile_id")
    WorkerProfile workerProfile;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    DocumentType type;
    @Column(name = "file_url", nullable = false, length = 500)
    String fileUrl;
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    void pre() {
        createdAt = LocalDateTime.now();
    }
}
