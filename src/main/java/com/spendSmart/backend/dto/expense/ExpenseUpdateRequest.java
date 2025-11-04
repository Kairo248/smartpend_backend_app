package com.spendSmart.backend.dto.expense;

import com.spendSmart.backend.entity.Expense;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseUpdateRequest {
    
    @NotNull(message = "Wallet ID is required")
    private Long walletId;
    
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g., USD)")
    private String currency;
    
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;
    
    @Size(max = 200, message = "Merchant name must not exceed 200 characters")
    private String merchant;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private String tagsJson;
    
    private String attachmentsJson;
    
    private Expense.ExpenseType type;
    
    private Boolean isRecurring;
}