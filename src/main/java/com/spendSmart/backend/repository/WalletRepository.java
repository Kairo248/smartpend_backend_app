package com.spendSmart.backend.repository;

import com.spendSmart.backend.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    List<Wallet> findByUserIdAndIsActiveTrue(Long userId);
    
    Optional<Wallet> findByIdAndUserIdAndIsActiveTrue(Long id, Long userId);
    
    Optional<Wallet> findByUserIdAndIsDefaultTrueAndIsActiveTrue(Long userId);
    
    boolean existsByUserIdAndNameAndIsActiveTrue(Long userId, String name);
}