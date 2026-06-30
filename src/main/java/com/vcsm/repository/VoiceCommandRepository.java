package com.vcsm.repository;

import com.vcsm.model.VoiceCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface VoiceCommandRepository extends JpaRepository<VoiceCommand, Long> {
    List<VoiceCommand> findByIntent(String intent);
    List<VoiceCommand> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT v.intent, COUNT(v) as intentCount FROM VoiceCommand v WHERE v.createdAt >= :startDate GROUP BY v.intent ORDER BY intentCount DESC")
    List<Object[]> findTopIntents(@Param("startDate") LocalDateTime startDate);
}
