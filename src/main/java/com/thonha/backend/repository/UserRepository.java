package com.thonha.backend.repository;

import com.thonha.backend.entity.UserStatus;
import com.thonha.backend.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    @Query("""
    SELECT u FROM Users u
    WHERE (:keyword IS NULL
           OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR u.phoneNumber LIKE CONCAT('%', :keyword, '%'))
      AND (:status IS NULL OR u.userStatus = :status)
      AND (:role IS NULL OR EXISTS (
            SELECT 1 FROM u.roles r WHERE r.name = :role))
      AND (:from IS NULL OR u.createdAt >= :from)
      AND (:to   IS NULL OR u.createdAt <= :to)
    """)
    Page<Users> search(@Param("keyword") String keyword,
                       @Param("status") UserStatus status,
                       @Param("role") String role,
                       @Param("from") LocalDateTime from,
                       @Param("to") LocalDateTime to,
                       Pageable pageable);


    Optional<Users> findByUsername(String username);
}
