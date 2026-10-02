package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "request_attachment")
@Getter
@Setter
@NoArgsConstructor
public class RequestAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    RepairRequest request;
    @Column(nullable = false, length = 20)
    String type;
    @Column(nullable = false, length = 500)
    String url;
    @Column(name = "sort_order", nullable = false)
    int sortOrder;
}
