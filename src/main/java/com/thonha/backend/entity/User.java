package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String fullName;
    
    @Column(name = "username", nullable = true, length = 50)
    private String username;
    
    @Column(unique = true, length = 100)
    private String email;
    
    @Column(nullable = false, unique = true, length = 15)
    private String phoneNumber;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    private String avatar;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "role_id"))
    private Set<Role> roles = new HashSet<>();
    
    public boolean hasRole(String roleName){
        return roles.stream().anyMatch(r->r.getName().equals(roleName));
    }
}
