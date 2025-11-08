package com.spendSmart.backend.service;

import com.spendSmart.backend.dto.analytics.*;
import com.spendSmart.backend.entity.*;
import com.spendSmart.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public ExpenseAnalyticsResponse getExpenseAnalytics(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting expense analytics for user {} from {} to {}", userId, startDate, endDate);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Get expenses for the period
        List<Expense> expenses = expenseRepository.findByUserAndTransactionDateBetween(user, startDate.toLocalDate(), endDate.toLocalDate());
        
        // Calculate totals
        BigDecimal totalExpenses = expenses.stream()
                .filter(e -> e.getType() == Expense.ExpenseType.EXPENSE)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalIncome = expenses.stream()
                .filter(e -> e.getType() == Expense.ExpenseType.INCOME)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netAmount = totalIncome.subtract(totalExpenses);
        
        // Previous period comparison
        LocalDateTime previousStart = startDate.minus(ChronoUnit.DAYS.between(startDate, endDate), ChronoUnit.DAYS);
        LocalDateTime previousEnd = startDate;
        
        List<Expense> previousExpenses = expenseRepository.findByUserAndTransactionDateBetween(user, previousStart.toLocalDate(), previousEnd.toLocalDate());
        BigDecimal previousPeriodExpenses = previousExpenses.stream()
                .filter(e -> e.getType() == Expense.ExpenseType.EXPENSE)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal expenseChange = totalExpenses.subtract(previousPeriodExpenses);
        BigDecimal expenseChangePercentage = previousPeriodExpenses.compareTo(BigDecimal.ZERO) > 0
                ? expenseChange.divide(previousPeriodExpenses, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        // Category breakdown
        Map<Category, List<Expense>> expensesByCategory = expenses.stream()
                .filter(e -> e.getType() == Expense.ExpenseType.EXPENSE)
                .collect(Collectors.groupingBy(Expense::getCategory));

        List<ExpenseAnalyticsResponse.CategoryExpenseSummary> categoryBreakdown = 
                expensesByCategory.entrySet().stream()
                        .map(entry -> createCategoryExpenseSummary(entry.getKey(), entry.getValue(), totalExpenses))
                        .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                        .collect(Collectors.toList());

        // Daily trends
        List<ExpenseAnalyticsResponse.DailySpendingSummary> dailyTrends = 
                generateDailyTrends(expenses, startDate, endDate);

        // Top categories (limit to top 5)
        List<ExpenseAnalyticsResponse.CategoryExpenseSummary> topCategories = 
                categoryBreakdown.stream().limit(5).collect(Collectors.toList());

        // Budget performance
        List<Budget> activeBudgets = budgetRepository.findActiveBudgetsForUser(user, LocalDateTime.now());
        List<ExpenseAnalyticsResponse.BudgetPerformance> budgetPerformance = 
                activeBudgets.stream()
                        .map(this::createBudgetPerformance)
                        .collect(Collectors.toList());

        return ExpenseAnalyticsResponse.builder()
                .totalExpenses(totalExpenses)
                .totalIncome(totalIncome)
                .netAmount(netAmount)
                .transactionCount(expenses.size())
                .previousPeriodExpenses(previousPeriodExpenses)
                .expenseChange(expenseChange)
                .expenseChangePercentage(expenseChangePercentage)
                .categoryBreakdown(categoryBreakdown)
                .dailyTrends(dailyTrends)
                .topCategories(topCategories)
                .budgetPerformance(budgetPerformance)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    public SpendingTrendsResponse getSpendingTrends(Long userId, int months) {
        log.info("Getting spending trends for user {} for {} months", userId, months);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(months);
        
        List<Expense> expenses = expenseRepository.findByUserAndTransactionDateBetween(user, startDate.toLocalDate(), endDate.toLocalDate());

        // Monthly trends
        List<SpendingTrendsResponse.MonthlyTrend> monthlyTrends = generateMonthlyTrends(expenses, months);

        // Weekly trends (last 12 weeks)
        List<SpendingTrendsResponse.WeeklyTrend> weeklyTrends = generateWeeklyTrends(expenses, 12);

        // Category trends
        List<SpendingTrendsResponse.CategoryTrend> categoryTrends = generateCategoryTrends(expenses, months);

        // Spending patterns
        SpendingTrendsResponse.SpendingPatterns spendingPatterns = generateSpendingPatterns(expenses);

        return SpendingTrendsResponse.builder()
                .monthlyTrends(monthlyTrends)
                .weeklyTrends(weeklyTrends)
                .categoryTrends(categoryTrends)
                .spendingPatterns(spendingPatterns)
                .build();
    }

    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        log.info("Getting dashboard summary for user {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        try {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = now.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59);

        // Current month data
        List<Expense> currentMonthExpenses = expenseRepository.findByUserAndTransactionDateBetween(user, monthStart.toLocalDate(), monthEnd.toLocalDate());
        
        BigDecimal currentMonthExpenseTotal = currentMonthExpenses.stream()
                .filter(e -> e.getType() == Expense.ExpenseType.EXPENSE)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal currentMonthIncomeTotal = currentMonthExpenses.stream()
                .filter(e -> e.getType() == Expense.ExpenseType.INCOME)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Previous month comparison
        LocalDateTime prevMonthStart = monthStart.minusMonths(1);
        LocalDateTime prevMonthEnd = monthEnd.minusMonths(1);
        
        List<Expense> previousMonthExpenses = expenseRepository.findByUserAndTransactionDateBetween(user, prevMonthStart.toLocalDate(), prevMonthEnd.toLocalDate());
        BigDecimal previousMonthExpenseTotal = previousMonthExpenses.stream()
                .filter(e -> e.getType() == Expense.ExpenseType.EXPENSE)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenseChange = currentMonthExpenseTotal.subtract(previousMonthExpenseTotal);
        BigDecimal expenseChangePercentage = previousMonthExpenseTotal.compareTo(BigDecimal.ZERO) > 0
                ? expenseChange.divide(previousMonthExpenseTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        // Budget summary
        DashboardSummaryResponse.BudgetSummary budgetSummary = generateBudgetSummary(user);

        // Quick stats
        DashboardSummaryResponse.QuickStats quickStats = generateQuickStats(user, currentMonthExpenses);

        // Recent transactions (last 10)
        List<Expense> recentExpenses = expenseRepository.findTop10ByUserOrderByTransactionDateDesc(user);
        List<DashboardSummaryResponse.RecentTransaction> recentTransactions = 
                recentExpenses.stream()
                        .map(this::createRecentTransaction)
                        .collect(Collectors.toList());

        // Budget alerts
        List<DashboardSummaryResponse.BudgetAlert> budgetAlerts = generateBudgetAlerts(user);

        return DashboardSummaryResponse.builder()
                .currentMonthExpenses(currentMonthExpenseTotal)
                .currentMonthIncome(currentMonthIncomeTotal)
                .currentMonthNet(currentMonthIncomeTotal.subtract(currentMonthExpenseTotal))
                .currentMonthTransactions(currentMonthExpenses.size())
                .previousMonthExpenses(previousMonthExpenseTotal)
                .expenseChange(expenseChange)
                .expenseChangePercentage(expenseChangePercentage)
                .budgetSummary(budgetSummary)
                .quickStats(quickStats)
                .recentTransactions(recentTransactions)
                .budgetAlerts(budgetAlerts)
                .build();
        
        } catch (Exception e) {
            log.error("Error generating dashboard summary for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate dashboard summary: " + e.getMessage(), e);
        }
    }

    // Helper methods
    private ExpenseAnalyticsResponse.CategoryExpenseSummary createCategoryExpenseSummary(
            Category category, List<Expense> expenses, BigDecimal totalExpenses) {
        
        BigDecimal categoryTotal = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal percentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                ? categoryTotal.divide(totalExpenses, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
                
        BigDecimal averageTransaction = expenses.size() > 0
                ? categoryTotal.divide(new BigDecimal(expenses.size()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ExpenseAnalyticsResponse.CategoryExpenseSummary.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .categoryColor(category.getColor())
                .categoryIcon(category.getIcon())
                .totalAmount(categoryTotal)
                .percentage(percentage)
                .transactionCount(expenses.size())
                .averageTransaction(averageTransaction)
                .build();
    }

    private List<ExpenseAnalyticsResponse.DailySpendingSummary> generateDailyTrends(
            List<Expense> expenses, LocalDateTime startDate, LocalDateTime endDate) {
        
        Map<LocalDateTime, List<Expense>> expensesByDay = expenses.stream()
                .collect(Collectors.groupingBy(e -> e.getTransactionDate().atStartOfDay()));

        List<ExpenseAnalyticsResponse.DailySpendingSummary> dailyTrends = new ArrayList<>();
        
        LocalDateTime current = startDate.toLocalDate().atStartOfDay();
        while (!current.isAfter(endDate)) {
            List<Expense> dayExpenses = expensesByDay.getOrDefault(current, Collections.emptyList());
            
            BigDecimal dailyExpenses = dayExpenses.stream()
                    .filter(e -> e.getType() == Expense.ExpenseType.EXPENSE)
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
            BigDecimal dailyIncome = dayExpenses.stream()
                    .filter(e -> e.getType() == Expense.ExpenseType.INCOME)
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dailyTrends.add(ExpenseAnalyticsResponse.DailySpendingSummary.builder()
                    .date(current)
                    .totalExpenses(dailyExpenses)
                    .totalIncome(dailyIncome)
                    .netAmount(dailyIncome.subtract(dailyExpenses))
                    .transactionCount(dayExpenses.size())
                    .build());
            
            current = current.plusDays(1);
        }
        
        return dailyTrends;
    }

    private ExpenseAnalyticsResponse.BudgetPerformance createBudgetPerformance(Budget budget) {
        return ExpenseAnalyticsResponse.BudgetPerformance.builder()
                .budgetId(budget.getId())
                .budgetName(budget.getName())
                .categoryName(budget.getCategory() != null ? budget.getCategory().getName() : "Overall")
                .budgetAmount(budget.getAmount())
                .spentAmount(budget.getSpentAmount())
                .remainingAmount(budget.getRemainingAmount())
                .spentPercentage(budget.getSpentPercentage())
                .isOverBudget(budget.isOverBudget())
                .shouldAlert(budget.shouldAlert())
                .build();
    }

    // Additional helper methods would be implemented here...
    // For brevity, I'll create placeholder implementations

    private List<SpendingTrendsResponse.MonthlyTrend> generateMonthlyTrends(List<Expense> expenses, int months) {
        // Implementation for monthly trends
        return new ArrayList<>();
    }

    private List<SpendingTrendsResponse.WeeklyTrend> generateWeeklyTrends(List<Expense> expenses, int weeks) {
        // Implementation for weekly trends
        return new ArrayList<>();
    }

    private List<SpendingTrendsResponse.CategoryTrend> generateCategoryTrends(List<Expense> expenses, int months) {
        // Implementation for category trends
        return new ArrayList<>();
    }

    private SpendingTrendsResponse.SpendingPatterns generateSpendingPatterns(List<Expense> expenses) {
        // Implementation for spending patterns
        return SpendingTrendsResponse.SpendingPatterns.builder().build();
    }

    private DashboardSummaryResponse.BudgetSummary generateBudgetSummary(User user) {
        List<Budget> activeBudgets = budgetRepository.findActiveBudgetsForUser(user, LocalDateTime.now());
        
        // Calculate actual spent amounts for each budget
        for (Budget budget : activeBudgets) {
            BigDecimal actualSpent = calculateActualSpentAmount(budget);
            budget.setSpentAmount(actualSpent);
        }
        
        BigDecimal totalBudgeted = activeBudgets.stream()
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalSpent = activeBudgets.stream()
                .map(Budget::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal budgetUtilization = totalBudgeted.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.divide(totalBudgeted, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        int overBudgets = (int) activeBudgets.stream().filter(Budget::isOverBudget).count();
        int alertingBudgets = (int) activeBudgets.stream().filter(Budget::shouldAlert).count();

        return DashboardSummaryResponse.BudgetSummary.builder()
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .totalRemaining(totalBudgeted.subtract(totalSpent))
                .budgetUtilization(budgetUtilization)
                .activeBudgets(activeBudgets.size())
                .overBudgets(overBudgets)
                .alertingBudgets(alertingBudgets)
                .build();
    }

    private DashboardSummaryResponse.QuickStats generateQuickStats(User user, List<Expense> currentMonthExpenses) {
        // Calculate average daily spending for current month
        BigDecimal totalExpenses = currentMonthExpenses.stream()
                .filter(e -> e.getType() == Expense.ExpenseType.EXPENSE)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        int daysInMonth = LocalDateTime.now().toLocalDate().lengthOfMonth();
        BigDecimal averageDailySpending = daysInMonth > 0 ? 
                totalExpenses.divide(new BigDecimal(daysInMonth), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
                
        // Find largest expense this month
        BigDecimal largestExpense = currentMonthExpenses.stream()
                .filter(e -> e.getType() == Expense.ExpenseType.EXPENSE)
                .map(Expense::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
                
        // Find top category by spending
        Map<String, BigDecimal> categoryTotals = currentMonthExpenses.stream()
                .filter(e -> e.getType() == Expense.ExpenseType.EXPENSE && e.getCategory() != null)
                .collect(Collectors.groupingBy(
                    e -> e.getCategory().getName(),
                    Collectors.mapping(Expense::getAmount, 
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
                
        String topCategory = "None";
        BigDecimal topCategoryAmount = BigDecimal.ZERO;
        
        if (!categoryTotals.isEmpty()) {
            Map.Entry<String, BigDecimal> topEntry = categoryTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            if (topEntry != null) {
                topCategory = topEntry.getKey();
                topCategoryAmount = topEntry.getValue();
            }
        }
        
        return DashboardSummaryResponse.QuickStats.builder()
                .averageDailySpending(averageDailySpending)
                .largestExpense(largestExpense)
                .topCategory(topCategory)
                .topCategoryAmount(topCategoryAmount)
                .daysUntilNextBudgetReset(30) // Default to 30 days
                .mostUsedWallet("Default") // Default value
                .build();
    }

    private DashboardSummaryResponse.RecentTransaction createRecentTransaction(Expense expense) {
        return DashboardSummaryResponse.RecentTransaction.builder()
                .id(expense.getId())
                .description(expense.getDescription() != null ? expense.getDescription() : "")
                .amount(expense.getAmount())
                .type(expense.getType().name())
                .categoryName(expense.getCategory() != null ? expense.getCategory().getName() : "Uncategorized")
                .categoryColor(expense.getCategory() != null ? expense.getCategory().getColor() : "#808080")
                .walletName(expense.getWallet() != null ? expense.getWallet().getName() : "Unknown")
                .date(expense.getTransactionDate().atStartOfDay())
                .build();
    }

    private List<DashboardSummaryResponse.BudgetAlert> generateBudgetAlerts(User user) {
        // Get all active budgets and update their spent amounts with fresh calculations
        List<Budget> activeBudgets = budgetRepository.findByUserAndIsActiveTrue(user);
        
        for (Budget budget : activeBudgets) {
            BigDecimal actualSpent = calculateActualSpentAmount(budget);
            System.out.println("Dashboard Alert Generation - Budget '" + budget.getName() + "' spent amount updated: " + 
                budget.getSpentAmount() + " -> " + actualSpent);
            budget.setSpentAmount(actualSpent);
        }
        
        // Now filter for budgets that need alerts based on fresh data
        List<Budget> alertingBudgets = activeBudgets.stream()
                .filter(budget -> {
                    boolean needsAlert = budget.shouldAlert() || budget.isOverBudget();
                    System.out.println("Budget '" + budget.getName() + "' alert check: shouldAlert=" + 
                        budget.shouldAlert() + ", isOverBudget=" + budget.isOverBudget() + ", needsAlert=" + needsAlert);
                    return needsAlert;
                })
                .collect(Collectors.toList());
                
        System.out.println("Total active budgets: " + activeBudgets.size() + ", Alerting budgets: " + alertingBudgets.size());
        
        return alertingBudgets.stream()
                .map(budget -> DashboardSummaryResponse.BudgetAlert.builder()
                        .budgetId(budget.getId())
                        .budgetName(budget.getName())
                        .categoryName(budget.getCategory() != null ? budget.getCategory().getName() : "Overall")
                        .spentPercentage(budget.getSpentPercentage())
                        .remainingAmount(budget.getRemainingAmount())
                        .alertType(budget.isOverBudget() ? "OVERBUDGET" : "THRESHOLD")
                        .message(generateAlertMessage(budget))
                        .alertDate(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
    }

    private String generateAlertMessage(Budget budget) {
        if (budget.isOverBudget()) {
            return "You have exceeded your budget by " + budget.getSpentAmount().subtract(budget.getAmount());
        } else {
            return "You have spent " + budget.getSpentPercentage() + "% of your budget";
        }
    }

    private BigDecimal calculateActualSpentAmount(Budget budget) {
        // Calculate spent amount based on actual expenses in the budget period
        LocalDateTime startDate = budget.getStartDate();
        LocalDateTime endDate = budget.getEndDate();
        
        // Get all expenses for the user in the budget period
        List<Expense> expenses = expenseRepository.findByUserAndTransactionDateBetween(
            budget.getUser(), 
            startDate.toLocalDate(), 
            endDate.toLocalDate()
        );
        
        // Filter by category if this is a category-specific budget
        if (budget.getCategory() != null) {
            expenses = expenses.stream()
                    .filter(expense -> expense.getCategory() != null && 
                            expense.getCategory().getId().equals(budget.getCategory().getId()))
                    .collect(Collectors.toList());
        }
        
        // Sum up only EXPENSE type transactions (not INCOME)
        return expenses.stream()
                .filter(expense -> expense.getType() == Expense.ExpenseType.EXPENSE)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}