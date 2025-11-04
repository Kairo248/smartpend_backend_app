package com.spendSmart.backend.dto.budget;

import com.spendSmart.backend.entity.Budget;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class UpdateBudgetRequest {

    @NotBlank(message = "Budget name is required")
    @Size(max = 100, message = "Budget name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Budget amount is required")
    @DecimalMin(value = "0.01", message = "Budget amount must be greater than 0")
    private BigDecimal amount;

    private Long categoryId; // Optional - null for overall budget

    @NotNull(message = "Period is required")
    private Budget.BudgetPeriod period;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @DecimalMin(value = "0.0", message = "Alert threshold must be between 0 and 100")
    private BigDecimal alertThreshold;

    private Boolean alertEnabled;

    private String description;

    private Boolean isActive;
}