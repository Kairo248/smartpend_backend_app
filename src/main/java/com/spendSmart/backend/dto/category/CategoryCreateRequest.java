package com.spendSmart.backend.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String name;
    
    @Size(max = 7, message = "Color must be in hex format (#RRGGBB)")
    private String color = "#6B7280";
    
    @Size(max = 50, message = "Icon name must not exceed 50 characters")
    private String icon;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private String rulePatterns;
}