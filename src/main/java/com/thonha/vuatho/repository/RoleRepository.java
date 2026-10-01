package com.thonha.vuatho.repository;

import com.thonha.vuatho.entity.Role;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);
}
