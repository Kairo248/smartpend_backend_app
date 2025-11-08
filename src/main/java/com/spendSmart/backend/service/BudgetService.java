package com.spendSmart.backend.service;

import com.spendSmart.backend.dto.budget.*;
import com.spendSmart.backend.entity.Budget;
import com.spendSmart.backend.entity.Category;
import com.spendSmart.backend.entity.User;
import com.spendSmart.backend.exception.ResourceNotFoundException;
import com.spendSmart.backend.exception.ValidationException;
import com.spendSmart.backend.mapper.BudgetMapper;
import com.spendSmart.backend.repository.BudgetRepository;
import com.spendSmart.backend.repository.CategoryRepository;
import com.spendSmart.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetMapper budgetMapper;
    private final UserRepository userRepository;
    private final com.spendSmart.backend.repository.ExpenseRepository expenseRepository;

    public List<BudgetResponse> getAllBudgets(Long userId) {
        log.info("Getting all budgets for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        List<Budget> budgets = budgetRepository.findByUserAndIsActiveTrue(user);
        
        // Update spent amounts for accurate status calculations
        for (Budget budget : budgets) {
            BigDecimal actualSpent = calculateActualSpentAmount(budget);
            log.info("Budget '{}' - Old spent: {}, New calculated spent: {}, Budget amount: {}", 
                budget.getName(), budget.getSpentAmount(), actualSpent, budget.getAmount());
            budget.setSpentAmount(actualSpent);
        }
        
        return budgets.stream()
                .map(this::mapToBudgetResponse)
                .toList();
    }

    public BudgetResponse getBudgetById(Long id, Long userId) {
        log.info("Getting budget {} for user: {}", id, userId);
        
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
                
        if (!budget.getUser().getId().equals(userId)) {
            throw new ValidationException("Budget does not belong to the current user");
        }
        
        // Update spent amount for accurate status
        BigDecimal actualSpent = calculateActualSpentAmount(budget);
        budget.setSpentAmount(actualSpent);
        
        return mapToBudgetResponse(budget);
    }

    public BudgetResponse createBudget(CreateBudgetRequest request, Long userId) {
        log.info("Creating budget for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        validateBudgetRequest(request, user, null);
        
        Budget budget = Budget.builder()
                .user(user)
                .category(getCategoryIfProvided(request.getCategoryId(), user))
                .name(request.getName())
                .amount(request.getAmount())
                .period(request.getPeriod())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .alertThreshold(request.getAlertThreshold())
                .alertEnabled(request.getAlertEnabled())
                .description(request.getDescription())
                .spentAmount(BigDecimal.ZERO)
                .isActive(true)
                .build();

        Budget savedBudget = budgetRepository.save(budget);
        log.info("Created budget with id: {}", savedBudget.getId());
        
        return mapToBudgetResponse(savedBudget);
    }

    public BudgetResponse updateBudget(Long id, UpdateBudgetRequest request, Long userId) {
        log.info("Updating budget {} for user: {}", id, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
                
        if (!budget.getUser().getId().equals(userId)) {
            throw new ValidationException("Budget does not belong to the current user");
        }
        
        validateBudgetUpdateRequest(request, user, id);
        
        // Update fields
        budget.setName(request.getName());
        budget.setAmount(request.getAmount());
        budget.setCategory(getCategoryIfProvided(request.getCategoryId(), user));
        budget.setPeriod(request.getPeriod());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setAlertThreshold(request.getAlertThreshold());
        budget.setAlertEnabled(request.getAlertEnabled());
        budget.setDescription(request.getDescription());
        
        if (request.getIsActive() != null) {
            budget.setIsActive(request.getIsActive());
        }

        Budget savedBudget = budgetRepository.save(budget);
        log.info("Updated budget with id: {}", savedBudget.getId());
        
        return mapToBudgetResponse(savedBudget);
    }

    public void deleteBudget(Long id, Long userId) {
        log.info("Deleting budget {} for user: {}", id, userId);
        
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
                
        if (!budget.getUser().getId().equals(userId)) {
            throw new ValidationException("Budget does not belong to the current user");
        }
        
        // Soft delete
        budget.setIsActive(false);
        budgetRepository.save(budget);
        
        log.info("Deleted budget with id: {}", id);
    }

    public BudgetSummaryResponse getBudgetSummary(Long userId) {
        log.info("Getting budget summary for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        List<Budget> activeBudgets = budgetRepository.findActiveBudgetsForUser(user, LocalDateTime.now());
        
        // Update all budgets with fresh spent amounts
        for (Budget budget : activeBudgets) {
            BigDecimal actualSpent = calculateActualSpentAmount(budget);
            budget.setSpentAmount(actualSpent);
        }
        
        // Now calculate summary statistics based on fresh data
        List<Budget> overBudgets = activeBudgets.stream()
                .filter(Budget::isOverBudget)
                .collect(Collectors.toList());
                
        List<Budget> alertingBudgets = activeBudgets.stream()
                .filter(Budget::shouldAlert)
                .collect(Collectors.toList());
        
        BigDecimal totalBudgeted = activeBudgets.stream()
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalSpent = activeBudgets.stream()
                .map(Budget::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalRemaining = totalBudgeted.subtract(totalSpent);
        
        BigDecimal overallSpentPercentage = totalBudgeted.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.divide(totalBudgeted, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        
        return BudgetSummaryResponse.builder()
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .totalRemaining(totalRemaining)
                .overallSpentPercentage(overallSpentPercentage)
                .totalBudgets(activeBudgets.size())
                .activeBudgets(activeBudgets.size())
                .overBudgetCount(overBudgets.size())
                .alertingBudgets(alertingBudgets.size())
                .budgets(activeBudgets.stream().map(this::mapToBudgetResponse).toList())
                .overBudgets(overBudgets.stream().map(this::mapToBudgetResponse).toList())
                .alertingBudgetsList(alertingBudgets.stream().map(this::mapToBudgetResponse).toList())
                .build();
    }

    public List<BudgetResponse> getActiveBudgets(Long userId) {
        log.info("Getting active budgets for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        List<Budget> activeBudgets = budgetRepository.findActiveBudgetsForUser(user, LocalDateTime.now());
        
        // Update spent amounts for accurate status calculations
        for (Budget budget : activeBudgets) {
            BigDecimal actualSpent = calculateActualSpentAmount(budget);
            budget.setSpentAmount(actualSpent);
        }
        
        return activeBudgets.stream()
                .map(this::mapToBudgetResponse)
                .toList();
    }

    // Helper methods
    private void validateBudgetRequest(CreateBudgetRequest request, User user, Long excludeId) {
        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date must be after start date");
        }
        
        // Check for overlapping budgets if category is specified
        if (request.getCategoryId() != null) {
            Category category = getCategoryIfProvided(request.getCategoryId(), user);
            
            boolean hasOverlapping = budgetRepository.existsOverlappingBudget(
                    user, category, request.getStartDate(), request.getEndDate(), 
                    excludeId != null ? excludeId : -1L);
                    
            if (hasOverlapping) {
                throw new ValidationException("A budget already exists for this category in the specified period");
            }
        }
        
        // Validate alert threshold
        if (request.getAlertThreshold() != null && 
            (request.getAlertThreshold().compareTo(BigDecimal.ZERO) < 0 || 
             request.getAlertThreshold().compareTo(new BigDecimal("100")) > 0)) {
            throw new ValidationException("Alert threshold must be between 0 and 100");
        }
    }

    private void validateBudgetUpdateRequest(UpdateBudgetRequest request, User user, Long budgetId) {
        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date must be after start date");
        }
        
        // Check for overlapping budgets if category is specified
        if (request.getCategoryId() != null) {
            Category category = getCategoryIfProvided(request.getCategoryId(), user);
            
            boolean hasOverlapping = budgetRepository.existsOverlappingBudget(
                    user, category, request.getStartDate(), request.getEndDate(), budgetId);
                    
            if (hasOverlapping) {
                throw new ValidationException("A budget already exists for this category in the specified period");
            }
        }
        
        // Validate alert threshold
        if (request.getAlertThreshold() != null && 
            (request.getAlertThreshold().compareTo(BigDecimal.ZERO) < 0 || 
             request.getAlertThreshold().compareTo(new BigDecimal("100")) > 0)) {
            throw new ValidationException("Alert threshold must be between 0 and 100");
        }
    }

    private Category getCategoryIfProvided(Long categoryId, User user) {
        if (categoryId == null) {
            return null;
        }
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
                
        // Check if category belongs to user or is a system category
        if (category.getUser() != null && !category.getUser().getId().equals(user.getId())) {
            throw new ValidationException("Category does not belong to the current user");
        }
        
        return category;
    }

    private BudgetResponse mapToBudgetResponse(Budget budget) {
        LocalDateTime now = LocalDateTime.now();
        Integer daysRemaining = budget.getEndDate().isAfter(now) 
                ? (int) ChronoUnit.DAYS.between(now, budget.getEndDate()) 
                : 0;
        
        // Note: Spent amount should already be calculated by the calling method
        
        // Debug logging
        log.info("Budget '{}' response - Spent: {}, Amount: {}, IsOver: {}, ShouldAlert: {}", 
            budget.getName(), budget.getSpentAmount(), budget.getAmount(), 
            budget.isOverBudget(), budget.shouldAlert());
            
        return BudgetResponse.builder()
                .id(budget.getId())
                .name(budget.getName())
                .amount(budget.getAmount())
                .spentAmount(budget.getSpentAmount())
                .remainingAmount(budget.getRemainingAmount())
                .spentPercentage(budget.getSpentPercentage())
                .category(budget.getCategory() != null ? budgetMapper.mapCategoryToResponse(budget.getCategory()) : null)
                .period(budget.getPeriod())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .isActive(budget.getIsActive())
                .alertThreshold(budget.getAlertThreshold())
                .alertEnabled(budget.getAlertEnabled())
                .description(budget.getDescription())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .isOverBudget(budget.isOverBudget())
                .shouldAlert(budget.shouldAlert())
                .isExpired(budget.getEndDate().isBefore(now))
                .daysRemaining(daysRemaining)
                .build();
    }

    @Transactional
    public void updateBudgetSpentAmounts(Long userId) {
        log.info("Updating budget spent amounts for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        List<Budget> activeBudgets = budgetRepository.findByUserAndIsActiveTrue(user);
        
        for (Budget budget : activeBudgets) {
            BigDecimal actualSpent = calculateActualSpentAmount(budget);
            budget.setSpentAmount(actualSpent);
            budgetRepository.save(budget);
        }
        
        log.info("Updated spent amounts for {} budgets", activeBudgets.size());
    }

    private BigDecimal calculateActualSpentAmount(Budget budget) {
        // Get all expenses for the user in the budget period
        List<com.spendSmart.backend.entity.Expense> expenses = expenseRepository.findByUserAndTransactionDateBetween(
            budget.getUser(), 
            budget.getStartDate().toLocalDate(), 
            budget.getEndDate().toLocalDate()
        );
        
        log.info("Budget '{}' calculation - Found {} expenses between {} and {}", 
            budget.getName(), expenses.size(), 
            budget.getStartDate().toLocalDate(), budget.getEndDate().toLocalDate());
            
        // Log all expenses for debugging
        expenses.forEach(expense -> {
            log.info("  - Expense: {} ({}), Amount: {}, Date: {}, Type: {}", 
                expense.getDescription(), 
                expense.getCategory() != null ? expense.getCategory().getName() : "No Category",
                expense.getAmount(), 
                expense.getTransactionDate(),
                expense.getType());
        });
        
        // Filter by category if this is a category-specific budget
        if (budget.getCategory() != null) {
            int beforeFilter = expenses.size();
            expenses = expenses.stream()
                    .filter(expense -> expense.getCategory() != null && 
                            expense.getCategory().getId().equals(budget.getCategory().getId()))
                    .collect(java.util.stream.Collectors.toList());
            log.info("Budget '{}' - Filtered by category '{}': {} -> {} expenses", 
                budget.getName(), budget.getCategory().getName(), beforeFilter, expenses.size());
        }
        
        // Sum up only EXPENSE type transactions (not INCOME)
        List<com.spendSmart.backend.entity.Expense> expenseTypeOnly = expenses.stream()
                .filter(expense -> expense.getType() == com.spendSmart.backend.entity.Expense.ExpenseType.EXPENSE)
                .collect(java.util.stream.Collectors.toList());
        
        log.info("Budget '{}' - After filtering EXPENSE type: {} expenses", budget.getName(), expenseTypeOnly.size());
        
        BigDecimal totalSpent = expenseTypeOnly.stream()
                .map(com.spendSmart.backend.entity.Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        log.info("Budget '{}' - Total calculated spent amount: {}", budget.getName(), totalSpent);
        return totalSpent;
    }
}