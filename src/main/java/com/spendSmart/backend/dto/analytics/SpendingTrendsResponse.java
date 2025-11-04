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
public class SpendingTrendsResponse {

    // Monthly trends
    private java.util.List<MonthlyTrend> monthlyTrends;
    
    // Weekly trends  
    private java.util.List<WeeklyTrend> weeklyTrends;
    
    // Category trends over time
    private java.util.List<CategoryTrend> categoryTrends;
    
    // Spending patterns
    private SpendingPatterns spendingPatterns;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyTrend {
        private Integer year;
        private Integer month;
        private String monthName;
        private BigDecimal totalExpenses;
        private BigDecimal totalIncome;
        private BigDecimal netAmount;
        private Integer transactionCount;
        private BigDecimal averageDaily;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyTrend {
        private Integer year;
        private Integer week;
        private LocalDateTime weekStart;
        private LocalDateTime weekEnd;
        private BigDecimal totalExpenses;
        private BigDecimal totalIncome;
        private BigDecimal netAmount;
        private Integer transactionCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryTrend {
        private Long categoryId;
        private String categoryName;
        private java.util.List<MonthlyTrend> monthlyData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpendingPatterns {
        private BigDecimal averageDailySpending;
        private BigDecimal averageWeeklySpending;
        private BigDecimal averageMonthlySpending;
        private String highestSpendingDay;
        private String lowestSpendingDay;
        private BigDecimal highestDayAmount;
        private BigDecimal lowestDayAmount;
        private java.util.List<DayOfWeekSpending> dayOfWeekPattern;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayOfWeekSpending {
        private String dayName;
        private Integer dayNumber;
        private BigDecimal averageAmount;
        private Integer transactionCount;
    }
}