package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_category")
@Getter
@Setter
@NoArgsConstructor
public class ServiceCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true, length = 100)
    String name;
    @Column(length = 500)
    String description;
    @Column(length = 255)
    String icon;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    CategoryStatus status = CategoryStatus.ACTIVE;
}
