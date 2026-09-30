package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
public class Role {
    public static final String CUSTOMER= "CUSTOMER";
    public static final String WORKER= "WORKER";
    public static final String ADMIN= "ADMIN";
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false,unique = true, length = 30)
    private String name;
    
    public Role(String name){
        this.name = name;
    }
}
