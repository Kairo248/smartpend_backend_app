package com.spendSmart.backend.dto.budget;

import com.spendSmart.backend.dto.category.CategoryResponse;
import com.spendSmart.backend.entity.Budget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponse {

    private Long id;
    private String name;
    private BigDecimal amount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal spentPercentage;
    private CategoryResponse category; // Null for overall budget
    private Budget.BudgetPeriod period;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private BigDecimal alertThreshold;
    private Boolean alertEnabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Status indicators
    private Boolean isOverBudget;
    private Boolean shouldAlert;
    private Boolean isExpired;
    
    // Additional computed fields
    private Integer daysRemaining;
}