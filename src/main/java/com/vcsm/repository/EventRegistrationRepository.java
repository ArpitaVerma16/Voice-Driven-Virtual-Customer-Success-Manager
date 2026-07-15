package com.vcsm.repository;

import com.vcsm.model.Event;
import com.vcsm.model.EventRegistration;
import com.vcsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByUserAndEvent(User user, Event event);
    Optional<EventRegistration> findByUserAndEvent(User user, Event event);
    Optional<EventRegistration> findByTicketToken(String ticketToken);
    List<EventRegistration> findByEvent(Event event);
    List<EventRegistration> findByUser(User user);
    void deleteByEvent(Event event);
}
