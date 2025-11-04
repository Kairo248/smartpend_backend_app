package com.spendSmart.backend.mapper;

import com.spendSmart.backend.dto.category.CategoryResponse;
import com.spendSmart.backend.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {

    public CategoryResponse mapCategoryToResponse(Category category) {
        if (category == null) {
            return null;
        }
        
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .icon(category.getIcon())
                .description(category.getDescription())
                .isSystem(category.getIsSystem())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}