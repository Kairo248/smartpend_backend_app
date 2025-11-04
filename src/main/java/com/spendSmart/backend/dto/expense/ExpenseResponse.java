package com.spendSmart.backend.dto.expense;

import com.spendSmart.backend.dto.category.CategoryResponse;
import com.spendSmart.backend.dto.wallet.WalletResponse;
import com.spendSmart.backend.entity.Expense;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private WalletResponse wallet;
    private CategoryResponse category;
    private BigDecimal amount;
    private String currency;
    private LocalDate transactionDate;
    private String merchant;
    private String description;
    private String tagsJson;
    private String attachmentsJson;
    private Expense.ExpenseType type;
    private Boolean isRecurring;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}