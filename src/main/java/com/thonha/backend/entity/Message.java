package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "message")
@Getter
@Setter
@NoArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    RepairRequest request;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    User sender;

    @Column(nullable = false, columnDefinition = "text")
    String content;

    @Column(name = "is_seen", nullable = false)
    boolean seen = false;

    @Column(name = "sent_at", nullable = false)
    LocalDateTime sentAt;

    @PrePersist
    void pre() {
        sentAt = LocalDateTime.now();
    }
}