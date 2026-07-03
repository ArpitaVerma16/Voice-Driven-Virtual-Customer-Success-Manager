package com.vcsm.repository;

import com.vcsm.model.ReminderQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReminderQueueRepository extends JpaRepository<ReminderQueue, Long> {
    List<ReminderQueue> findByStatusAndScheduledAtBefore(String status, LocalDateTime time);
    List<ReminderQueue> findByEventIdAndUserIdAndReminderType(Long eventId, Long userId, String reminderType);
}
