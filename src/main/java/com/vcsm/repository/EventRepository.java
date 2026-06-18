package com.vcsm.repository;

import com.vcsm.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByActiveTrue();

    List<Event> findByEventDateAfterAndActiveTrue(LocalDateTime dateTime);

    List<Event> findByCategoryAndActiveTrue(Event.EventCategory category);

    List<Event> findByEventDateAfter(LocalDateTime dateTime);
}