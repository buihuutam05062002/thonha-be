package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_category")
@Getter
@Setter
@NoArgsConstructor
public class ServiceCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    private String description;
    
    private String icon;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private CategoryStatus status = CategoryStatus.ACTIVE;
}
