package com.spendSmart.backend.controller;

import com.spendSmart.backend.dto.category.*;
import com.spendSmart.backend.security.UserPrincipal;
import com.spendSmart.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getUserCategories(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<CategoryResponse> categories = categoryService.getUserCategories(userPrincipal.getId());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CategoryResponse category = categoryService.getCategoryById(id, userPrincipal.getId());
        return ResponseEntity.ok(category);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CategoryResponse category = categoryService.createCategory(request, userPrincipal.getId());
        return ResponseEntity.ok(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CategoryResponse category = categoryService.updateCategory(id, request, userPrincipal.getId());
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        categoryService.deleteCategory(id, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }
}