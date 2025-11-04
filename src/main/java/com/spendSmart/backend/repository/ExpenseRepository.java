package com.spendSmart.backend.repository;

import com.spendSmart.backend.entity.Expense;
import com.spendSmart.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    List<Expense> findByUserIdOrderByTransactionDateDesc(Long userId);
    
    Optional<Expense> findByIdAndUserId(Long id, Long userId);
    
    List<Expense> findByUserIdAndWalletIdOrderByTransactionDateDesc(Long userId, Long walletId);
    
    List<Expense> findByUserIdAndCategoryIdOrderByTransactionDateDesc(Long userId, Long categoryId);
    
    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId AND e.transactionDate BETWEEN :startDate AND :endDate ORDER BY e.transactionDate DESC")
    List<Expense> findByUserIdAndTransactionDateBetween(@Param("userId") Long userId, 
                                                       @Param("startDate") LocalDate startDate, 
                                                       @Param("endDate") LocalDate endDate);
    
    // Methods for Analytics Service
    List<Expense> findByUserAndTransactionDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    List<Expense> findTop10ByUserOrderByTransactionDateDesc(User user);
}