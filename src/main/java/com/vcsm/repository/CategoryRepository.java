package com.vcsm.repository;

import com.vcsm.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findByParentIsNullOrderByDisplayOrderAsc();
    
    List<Category> findByParentIdOrderByDisplayOrderAsc(Long parentId);
    
    List<Category> findByActiveTrueOrderByNameAsc();
    
    List<Category> findByParentIsNullAndActiveTrueOrderByDisplayOrderAsc();
}