package com.vcsm.repository;

import com.vcsm.model.Notification;
import com.vcsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    
    List<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);
    
    long countByUserAndReadFalse(User user);
}