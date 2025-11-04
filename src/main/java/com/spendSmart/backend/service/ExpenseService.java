package com.spendSmart.backend.service;

import com.spendSmart.backend.dto.expense.*;
import com.spendSmart.backend.entity.Category;
import com.spendSmart.backend.entity.Expense;
import com.spendSmart.backend.entity.User;
import com.spendSmart.backend.entity.Wallet;
import com.spendSmart.backend.repository.CategoryRepository;
import com.spendSmart.backend.repository.ExpenseRepository;
import com.spendSmart.backend.repository.UserRepository;
import com.spendSmart.backend.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private CategoryService categoryService;

    public List<ExpenseResponse> getUserExpenses(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdOrderByTransactionDateDesc(userId);
        return expenses.stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    public List<ExpenseResponse> getExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = expenseRepository.findByUserIdAndTransactionDateBetween(userId, startDate, endDate);
        return expenses.stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    public ExpenseResponse getExpenseById(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        return mapToExpenseResponse(expense);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = walletRepository.findByIdAndUserIdAndIsActiveTrue(request.getWalletId(), userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Category category = validateCategory(request.getCategoryId(), userId);

        Expense expense = Expense.builder()
                .user(user)
                .wallet(wallet)
                .category(category)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .transactionDate(request.getTransactionDate())
                .merchant(request.getMerchant())
                .description(request.getDescription())
                .tagsJson(request.getTagsJson())
                .attachmentsJson(request.getAttachmentsJson())
                .type(request.getType())
                .isRecurring(request.getIsRecurring())
                .build();

        Expense savedExpense = expenseRepository.save(expense);

        // Update wallet balance based on expense type
        updateWalletBalance(wallet, savedExpense, true);

        return mapToExpenseResponse(savedExpense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, ExpenseUpdateRequest request, Long userId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        Wallet oldWallet = expense.getWallet();

        // Revert old wallet balance change
        updateWalletBalance(oldWallet, expense, false);

        Wallet newWallet = walletRepository.findByIdAndUserIdAndIsActiveTrue(request.getWalletId(), userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Category category = validateCategory(request.getCategoryId(), userId);

        expense.setWallet(newWallet);
        expense.setCategory(category);
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency());
        expense.setTransactionDate(request.getTransactionDate());
        expense.setMerchant(request.getMerchant());
        expense.setDescription(request.getDescription());
        expense.setTagsJson(request.getTagsJson());
        expense.setAttachmentsJson(request.getAttachmentsJson());
        
        if (request.getType() != null) {
            expense.setType(request.getType());
        }
        
        if (request.getIsRecurring() != null) {
            expense.setIsRecurring(request.getIsRecurring());
        }

        Expense updatedExpense = expenseRepository.save(expense);

        // Apply new wallet balance change
        updateWalletBalance(newWallet, updatedExpense, true);

        return mapToExpenseResponse(updatedExpense);
    }

    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // Revert wallet balance change
        updateWalletBalance(expense.getWallet(), expense, false);

        expenseRepository.delete(expense);
    }

    private Category validateCategory(Long categoryId, Long userId) {
        // Check if it's a system category
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category != null && category.getIsSystem() && category.getIsActive()) {
            return category;
        }

        // Check if it's a user category
        return categoryRepository.findByIdAndUserIdAndIsActiveTrue(categoryId, userId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    private void updateWalletBalance(Wallet wallet, Expense expense, boolean isAdding) {
        BigDecimal amount = expense.getAmount();
        
        switch (expense.getType()) {
            case EXPENSE:
                if (isAdding) {
                    wallet.subtractFromBalance(amount);
                } else {
                    wallet.addToBalance(amount);
                }
                break;
            case INCOME:
                if (isAdding) {
                    wallet.addToBalance(amount);
                } else {
                    wallet.subtractFromBalance(amount);
                }
                break;
            case TRANSFER:
                // For transfers, we'll handle this in a future enhancement
                break;
        }
        
        walletRepository.save(wallet);
    }

    private ExpenseResponse mapToExpenseResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                walletService.getWalletById(expense.getWallet().getId(), expense.getUser().getId()),
                categoryService.getCategoryById(expense.getCategory().getId(), expense.getUser().getId()),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getTransactionDate(),
                expense.getMerchant(),
                expense.getDescription(),
                expense.getTagsJson(),
                expense.getAttachmentsJson(),
                expense.getType(),
                expense.getIsRecurring(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}