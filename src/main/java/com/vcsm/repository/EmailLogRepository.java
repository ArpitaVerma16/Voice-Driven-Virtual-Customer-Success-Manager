package com.vcsm.repository;

import com.vcsm.model.EmailLog;
import com.vcsm.model.Event;
import com.vcsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    
    List<EmailLog> findByEvent(Event event);
    
    List<EmailLog> findByUser(User user);
    
    List<EmailLog> findByStatusAndSentAtBefore(String status, LocalDateTime date);
    
    long countByEventAndStatus(Event event, String status);
    void deleteByEvent(Event event);
}