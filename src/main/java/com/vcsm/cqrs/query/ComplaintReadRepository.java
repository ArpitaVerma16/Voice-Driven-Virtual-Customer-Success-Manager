package com.vcsm.cqrs.query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintReadRepository extends JpaRepository<ComplaintReadModel, Long> {
    
    Optional<ComplaintReadModel> findById(Long id);
    
    List<ComplaintReadModel> findAll();
    
    List<ComplaintReadModel> findByStatus(String status);
    
    List<ComplaintReadModel> findByCategory(String category);
    
    List<ComplaintReadModel> findByPriority(String priority);
    
    List<ComplaintReadModel> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<ComplaintReadModel> findByStatusAndCategory(String status, String category);
}