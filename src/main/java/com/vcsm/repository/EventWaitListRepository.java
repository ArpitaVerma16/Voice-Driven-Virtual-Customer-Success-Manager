package com.vcsm.repository;

import com.vcsm.model.Event;
import com.vcsm.model.EventWaitlist;
import com.vcsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventWaitlistRepository extends JpaRepository<EventWaitlist, Long> {
    
    List<EventWaitlist> findByEventOrderByJoinedAtAsc(Event event);
    
    Optional<EventWaitlist> findByEventAndUser(Event event, User user);
    
    List<EventWaitlist> findByEventAndConfirmedFalseAndExpiresAtAfter(Event event, LocalDateTime now);
    
    List<EventWaitlist> findByConfirmedFalseAndExpiresAtBefore(LocalDateTime now);
    
    long countByEventAndConfirmedFalse(Event event);
    
    Optional<EventWaitlist> findFirstByEventAndConfirmedFalseOrderByJoinedAtAsc(Event event);
    
    void deleteByEventAndUser(Event event, User user);
}