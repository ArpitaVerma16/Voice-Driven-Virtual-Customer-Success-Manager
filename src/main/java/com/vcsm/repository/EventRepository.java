package com.vcsm.repository;

import com.vcsm.model.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Loads an event with a pessimistic write lock, serializing concurrent
     * registrations for the same event so the duplicate-registration and
     * capacity checks cannot race between two requests.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findWithLockById(@Param("id") Long id);

    List<Event> findByActiveTrue();

    List<Event> findByEventDateAfterAndActiveTrue(LocalDateTime dateTime);

    List<Event> findByCategoryAndActiveTrue(Event.EventCategory category);

    List<Event> findByEventDateAfter(LocalDateTime dateTime);

    // Count queries for dashboard cards: the dashboard only needs the
    // numbers, so counting in the database avoids loading every event row.
    long countByActiveTrue();

    long countByEventDateAfterAndActiveTrue(LocalDateTime dateTime);
}