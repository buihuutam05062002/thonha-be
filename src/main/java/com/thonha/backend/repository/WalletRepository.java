package com.thonha.backend.repository;
import com.thonha.backend.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    boolean existsByWorkerId(Long workerId);
}