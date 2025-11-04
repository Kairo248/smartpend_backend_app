package com.spendSmart.backend.repository;

import com.spendSmart.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findByUserIdAndIsActiveTrue(Long userId);
    
    List<Category> findByIsSystemTrueAndIsActiveTrue();
    
    Optional<Category> findByIdAndUserIdAndIsActiveTrue(Long id, Long userId);
    
    boolean existsByUserIdAndNameAndIsActiveTrue(Long userId, String name);
}