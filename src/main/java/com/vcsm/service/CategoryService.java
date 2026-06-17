package com.vcsm.service;

import com.vcsm.model.Category;
import com.vcsm.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    public List<Category> getParentCategories() {
        return categoryRepository.findByParentIsNullOrderByDisplayOrderAsc();
    }
    
    public List<Category> getSubCategories(Long parentId) {
        return categoryRepository.findByParentIdOrderByDisplayOrderAsc(parentId);
    }
    
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }
    
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }
    
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = getCategoryById(id);
        if (category == null) return null;
        
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setColorCode(categoryDetails.getColorCode());
        category.setActive(categoryDetails.isActive());
        category.setDisplayOrder(categoryDetails.getDisplayOrder());
        
        return categoryRepository.save(category);
    }
    
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        if (category != null) {
            // Move sub-categories to parent's parent
            for (Category sub : category.getSubCategories()) {
                sub.setParent(category.getParent());
                categoryRepository.save(sub);
            }
            categoryRepository.delete(category);
        }
    }
    
    public Map<String, List<String>> getCategoryTree() {
        Map<String, List<String>> tree = new HashMap<>();
        List<Category> parents = getParentCategories();
        
        for (Category parent : parents) {
            List<String> subs = parent.getSubCategories().stream()
                .map(Category::getName)
                .toList();
            tree.put(parent.getName(), subs);
        }
        return tree;
    }
}