package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "worker_specialty", uniqueConstraints = @UniqueConstraint(columnNames = {"worker_profile_id", "service_category_id"}))
@Getter
@Setter
@NoArgsConstructor
public class WorkerSpecialty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_profile_id", nullable = false)
    private WorkerProfile workerProfile;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_category_id", nullable = false)
    private ServiceCategory serviceCategory;
}
