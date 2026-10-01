package com.thonha.vuatho.repository;

import com.thonha.vuatho.entity.User;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhoneNumber(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneNumber(String phone);
}
