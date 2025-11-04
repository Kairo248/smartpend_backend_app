package com.spendSmart.backend.repository;

import com.spendSmart.backend.entity.Budget;
import com.spendSmart.backend.entity.Category;
import com.spendSmart.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // Find all budgets for a user
    List<Budget> findByUserAndIsActiveTrue(User user);

    // Find budget by user and category
    Optional<Budget> findByUserAndCategoryAndIsActiveTrue(User user, Category category);

    // Find overall budget (no category) for user
    Optional<Budget> findByUserAndCategoryIsNullAndIsActiveTrue(User user);

    // Find budgets that are currently active (within date range)
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND :currentDate BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsForUser(@Param("user") User user, @Param("currentDate") LocalDateTime currentDate);

    // Find budgets that need alerts (spent amount >= alert threshold)
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND b.alertEnabled = true AND " +
           "(b.spentAmount * 100 / b.amount) >= b.alertThreshold")
    List<Budget> findBudgetsNeedingAlert(@Param("user") User user);

    // Find overbudget budgets
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND b.spentAmount > b.amount")
    List<Budget> findOverbudgetBudgets(@Param("user") User user);

    // Find budgets by period
    List<Budget> findByUserAndPeriodAndIsActiveTrue(User user, Budget.BudgetPeriod period);

    // Find budgets ending soon (within next 7 days)
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND " +
           "b.endDate BETWEEN :startDate AND :endDate")
    List<Budget> findBudgetsEndingSoon(@Param("user") User user, 
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // Check if budget exists for category in overlapping period
    @Query("SELECT COUNT(b) > 0 FROM Budget b WHERE b.user = :user AND b.category = :category AND " +
           "b.isActive = true AND b.id != :excludeId AND " +
           "((b.startDate <= :endDate AND b.endDate >= :startDate))")
    boolean existsOverlappingBudget(@Param("user") User user, 
                                  @Param("category") Category category,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  @Param("excludeId") Long excludeId);

    // Find all budgets for a specific category across all users (admin function)
    List<Budget> findByCategoryAndIsActiveTrue(Category category);
}