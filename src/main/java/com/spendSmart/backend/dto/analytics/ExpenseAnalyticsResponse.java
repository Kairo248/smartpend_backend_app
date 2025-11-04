package com.spendSmart.backend.dto.analytics;

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
public class ExpenseAnalyticsResponse {

    // Summary totals
    private BigDecimal totalExpenses;
    private BigDecimal totalIncome;
    private BigDecimal netAmount;
    private Integer transactionCount;
    
    // Period comparisons
    private BigDecimal previousPeriodExpenses;
    private BigDecimal expenseChange;
    private BigDecimal expenseChangePercentage;
    
    // Category breakdown
    private java.util.List<CategoryExpenseSummary> categoryBreakdown;
    
    // Daily spending trend
    private java.util.List<DailySpendingSummary> dailyTrends;
    
    // Top categories
    private java.util.List<CategoryExpenseSummary> topCategories;
    
    // Budget performance
    private java.util.List<BudgetPerformance> budgetPerformance;
    
    // Analysis period
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryExpenseSummary {
        private Long categoryId;
        private String categoryName;
        private String categoryColor;
        private String categoryIcon;
        private BigDecimal totalAmount;
        private BigDecimal percentage;
        private Integer transactionCount;
        private BigDecimal averageTransaction;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailySpendingSummary {
        private LocalDateTime date;
        private BigDecimal totalExpenses;
        private BigDecimal totalIncome;
        private BigDecimal netAmount;
        private Integer transactionCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BudgetPerformance {
        private Long budgetId;
        private String budgetName;
        private String categoryName;
        private BigDecimal budgetAmount;
        private BigDecimal spentAmount;
        private BigDecimal remainingAmount;
        private BigDecimal spentPercentage;
        private Boolean isOverBudget;
        private Boolean shouldAlert;
    }
}