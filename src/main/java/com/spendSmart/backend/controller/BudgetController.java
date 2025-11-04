package com.spendSmart.backend.controller;

import com.spendSmart.backend.dto.budget.*;
import com.spendSmart.backend.security.UserPrincipal;
import com.spendSmart.backend.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAllBudgets(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<BudgetResponse> budgets = budgetService.getAllBudgets(userPrincipal.getId());
        return ResponseEntity.ok(budgets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BudgetResponse budget = budgetService.getBudgetById(id, userPrincipal.getId());
        return ResponseEntity.ok(budget);
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody CreateBudgetRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BudgetResponse budget = budgetService.createBudget(request, userPrincipal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(budget);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBudgetRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BudgetResponse budget = budgetService.updateBudget(id, request, userPrincipal.getId());
        return ResponseEntity.ok(budget);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        budgetService.deleteBudget(id, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<BudgetSummaryResponse> getBudgetSummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BudgetSummaryResponse summary = budgetService.getBudgetSummary(userPrincipal.getId());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/active")
    public ResponseEntity<List<BudgetResponse>> getActiveBudgets(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<BudgetResponse> budgets = budgetService.getActiveBudgets(userPrincipal.getId());
        return ResponseEntity.ok(budgets);
    }
}