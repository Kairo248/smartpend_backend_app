package com.spendSmart.backend.service;

import com.spendSmart.backend.dto.category.*;
import com.spendSmart.backend.entity.Category;
import com.spendSmart.backend.entity.User;
import com.spendSmart.backend.repository.CategoryRepository;
import com.spendSmart.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    @Transactional
    public void initializeSystemCategories() {
        if (categoryRepository.findByIsSystemTrueAndIsActiveTrue().isEmpty()) {
            createSystemCategories();
        }
    }

    public List<CategoryResponse> getUserCategories(Long userId) {
        List<Category> systemCategories = categoryRepository.findByIsSystemTrueAndIsActiveTrue();
        List<Category> userCategories = categoryRepository.findByUserIdAndIsActiveTrue(userId);
        
        List<Category> allCategories = List.of(systemCategories, userCategories)
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        
        return allCategories.stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdAndUserIdAndIsActiveTrue(categoryId, userId)
                .or(() -> categoryRepository.findById(categoryId)
                        .filter(c -> c.getIsSystem() && c.getIsActive()))
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return mapToCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if category name already exists for this user
        if (categoryRepository.existsByUserIdAndNameAndIsActiveTrue(userId, request.getName())) {
            throw new RuntimeException("Category with this name already exists");
        }

        Category category = Category.builder()
                .user(user)
                .name(request.getName())
                .color(request.getColor())
                .icon(request.getIcon())
                .description(request.getDescription())
                .rulePatterns(request.getRulePatterns())
                .isSystem(false)
                .isActive(true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request, Long userId) {
        Category category = categoryRepository.findByIdAndUserIdAndIsActiveTrue(categoryId, userId)
                .orElseThrow(() -> new RuntimeException("Category not found or not owned by user"));

        // System categories cannot be updated by users
        if (category.getIsSystem()) {
            throw new RuntimeException("System categories cannot be modified");
        }

        // Check if name already exists (excluding current category)
        if (!category.getName().equals(request.getName()) && 
            categoryRepository.existsByUserIdAndNameAndIsActiveTrue(userId, request.getName())) {
            throw new RuntimeException("Category with this name already exists");
        }

        category.setName(request.getName());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        category.setDescription(request.getDescription());
        category.setRulePatterns(request.getRulePatterns());
        
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        Category updatedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdAndUserIdAndIsActiveTrue(categoryId, userId)
                .orElseThrow(() -> new RuntimeException("Category not found or not owned by user"));

        // System categories cannot be deleted
        if (category.getIsSystem()) {
            throw new RuntimeException("System categories cannot be deleted");
        }

        // Soft delete
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    private void createSystemCategories() {
        List<Category> systemCategories = Arrays.asList(
            createSystemCategory("Food & Dining", "#EF4444", "utensils", "Restaurant meals, grocery shopping, food delivery"),
            createSystemCategory("Transportation", "#3B82F6", "car", "Gas, public transport, taxi, car maintenance"),
            createSystemCategory("Entertainment", "#8B5CF6", "gamepad", "Movies, concerts, games, subscriptions"),
            createSystemCategory("Shopping", "#F59E0B", "shopping-bag", "Clothing, electronics, personal items"),
            createSystemCategory("Bills & Utilities", "#10B981", "receipt", "Electricity, water, internet, phone bills"),
            createSystemCategory("Healthcare", "#EC4899", "heart", "Doctor visits, medicine, health insurance"),
            createSystemCategory("Education", "#6366F1", "book", "Tuition, books, courses, training"),
            createSystemCategory("Travel", "#14B8A6", "plane", "Flights, hotels, vacation expenses"),
            createSystemCategory("Income", "#22C55E", "dollar-sign", "Salary, freelance, investments"),
            createSystemCategory("Other", "#6B7280", "more-horizontal", "Miscellaneous expenses")
        );

        categoryRepository.saveAll(systemCategories);
    }

    private Category createSystemCategory(String name, String color, String icon, String description) {
        return Category.builder()
                .user(null) // System categories don't belong to any user
                .name(name)
                .color(color)
                .icon(icon)
                .description(description)
                .isSystem(true)
                .isActive(true)
                .build();
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getIcon(),
                category.getDescription(),
                category.getRulePatterns(),
                category.getIsSystem(),
                category.getIsActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}