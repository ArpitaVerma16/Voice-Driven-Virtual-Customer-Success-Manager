package com.vcsm.repository;

import com.vcsm.model.UserActivity;
import com.vcsm.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    
    List<UserActivity> findByUserOrderByCreatedAtDesc(User user);
    
    Page<UserActivity> findByUser(User user, Pageable pageable);
    
    List<UserActivity> findByUserAndActionTypeOrderByCreatedAtDesc(User user, String actionType);
    
    List<UserActivity> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, LocalDateTime start, LocalDateTime end);
    
    long countByUser(User user);
}