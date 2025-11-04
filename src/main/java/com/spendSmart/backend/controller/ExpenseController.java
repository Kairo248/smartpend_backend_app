package com.spendSmart.backend.controller;

import com.spendSmart.backend.dto.expense.*;
import com.spendSmart.backend.security.UserPrincipal;
import com.spendSmart.backend.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getUserExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<ExpenseResponse> expenses;
        if (startDate != null && endDate != null) {
            expenses = expenseService.getExpensesByDateRange(userPrincipal.getId(), startDate, endDate);
        } else {
            expenses = expenseService.getUserExpenses(userPrincipal.getId());
        }
        
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpense(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ExpenseResponse expense = expenseService.getExpenseById(id, userPrincipal.getId());
        return ResponseEntity.ok(expense);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ExpenseResponse expense = expenseService.createExpense(request, userPrincipal.getId());
        return ResponseEntity.ok(expense);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ExpenseResponse expense = expenseService.updateExpense(id, request, userPrincipal.getId());
        return ResponseEntity.ok(expense);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        expenseService.deleteExpense(id, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }
}