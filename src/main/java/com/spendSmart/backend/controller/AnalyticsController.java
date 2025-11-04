package com.spendSmart.backend.controller;

import com.spendSmart.backend.dto.analytics.*;
import com.spendSmart.backend.security.UserPrincipal;
import com.spendSmart.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/expenses")
    public ResponseEntity<ExpenseAnalyticsResponse> getExpenseAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        ExpenseAnalyticsResponse analytics = analyticsService.getExpenseAnalytics(userPrincipal.getId(), startDate, endDate);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/trends")
    public ResponseEntity<SpendingTrendsResponse> getSpendingTrends(
            @RequestParam(defaultValue = "12") int months,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        SpendingTrendsResponse trends = analyticsService.getSpendingTrends(userPrincipal.getId(), months);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        DashboardSummaryResponse summary = analyticsService.getDashboardSummary(userPrincipal.getId());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/expenses/current-month")
    public ResponseEntity<ExpenseAnalyticsResponse> getCurrentMonthAnalytics(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
        
        ExpenseAnalyticsResponse analytics = analyticsService.getExpenseAnalytics(userPrincipal.getId(), monthStart, monthEnd);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/expenses/last-month")
    public ResponseEntity<ExpenseAnalyticsResponse> getLastMonthAnalytics(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastMonthStart = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime lastMonthEnd = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
        
        ExpenseAnalyticsResponse analytics = analyticsService.getExpenseAnalytics(userPrincipal.getId(), lastMonthStart, lastMonthEnd);
        return ResponseEntity.ok(analytics);
    }
}