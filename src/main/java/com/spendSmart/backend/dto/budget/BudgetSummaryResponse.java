package com.spendSmart.backend.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetSummaryResponse {

    private BigDecimal totalBudgeted;
    private BigDecimal totalSpent;
    private BigDecimal totalRemaining;
    private BigDecimal overallSpentPercentage;
    
    private Integer totalBudgets;
    private Integer activeBudgets;
    private Integer overBudgetCount;
    private Integer alertingBudgets;
    
    private List<BudgetResponse> budgets;
    private List<BudgetResponse> overBudgets;
    private List<BudgetResponse> alertingBudgetsList;
}