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
public class DashboardSummaryResponse {

    // Current month summary
    private BigDecimal currentMonthExpenses;
    private BigDecimal currentMonthIncome;
    private BigDecimal currentMonthNet;
    private Integer currentMonthTransactions;
    
    // Previous month comparison
    private BigDecimal previousMonthExpenses;
    private BigDecimal expenseChange;
    private BigDecimal expenseChangePercentage;
    
    // Budget summary
    private BudgetSummary budgetSummary;
    
    // Quick stats
    private QuickStats quickStats;
    
    // Recent activity
    private java.util.List<RecentTransaction> recentTransactions;
    
    // Upcoming budget alerts
    private java.util.List<BudgetAlert> budgetAlerts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BudgetSummary {
        private BigDecimal totalBudgeted;
        private BigDecimal totalSpent;
        private BigDecimal totalRemaining;
        private BigDecimal budgetUtilization;
        private Integer activeBudgets;
        private Integer overBudgets;
        private Integer alertingBudgets;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuickStats {
        private BigDecimal averageDailySpending;
        private BigDecimal largestExpense;
        private String topCategory;
        private BigDecimal topCategoryAmount;
        private Integer daysUntilNextBudgetReset;
        private String mostUsedWallet;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentTransaction {
        private Long id;
        private String description;
        private BigDecimal amount;
        private String type; // EXPENSE, INCOME
        private String categoryName;
        private String categoryColor;
        private String walletName;
        private LocalDateTime date;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BudgetAlert {
        private Long budgetId;
        private String budgetName;
        private String categoryName;
        private BigDecimal spentPercentage;
        private BigDecimal remainingAmount;
        private String alertType; // THRESHOLD, OVERBUDGET, EXPIRING
        private String message;
        private LocalDateTime alertDate;
    }
}