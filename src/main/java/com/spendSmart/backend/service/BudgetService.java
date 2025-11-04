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


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetMapper budgetMapper;
    private final UserRepository userRepository;

    public List<BudgetResponse> getAllBudgets(Long userId) {
        log.info("Getting all budgets for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        List<Budget> budgets = budgetRepository.findByUserAndIsActiveTrue(user);
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
        List<Budget> overBudgets = budgetRepository.findOverbudgetBudgets(user);
        List<Budget> alertingBudgets = budgetRepository.findBudgetsNeedingAlert(user);
        
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
}