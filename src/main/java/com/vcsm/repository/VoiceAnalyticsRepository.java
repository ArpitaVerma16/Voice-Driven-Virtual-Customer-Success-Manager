package com.vcsm.repository;

import com.vcsm.model.VoiceAnalytics;
import com.vcsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Repository
public interface VoiceAnalyticsRepository extends JpaRepository<VoiceAnalytics, Long> {
    

    List<VoiceAnalytics> findByUserOrderByCreatedAtDesc(User user);

    List<VoiceAnalytics> findByUser(User user);

    
    List<VoiceAnalytics> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT v.intent, COUNT(v) FROM VoiceAnalytics v GROUP BY v.intent ORDER BY COUNT(v) DESC")
    List<Object[]> countByIntent();
    
    @Query("SELECT FUNCTION('HOUR', v.createdAt) as hour, COUNT(v) FROM VoiceAnalytics v GROUP BY FUNCTION('HOUR', v.createdAt) ORDER BY hour")
    List<Object[]> countByHour();
    
    @Query("SELECT v.success, COUNT(v) FROM VoiceAnalytics v GROUP BY v.success")
    List<Object[]> countBySuccess();
    
    @Query("SELECT AVG(v.responseTime) FROM VoiceAnalytics v")
    Double getAverageResponseTime();
    
    @Query("SELECT COUNT(DISTINCT v.user) FROM VoiceAnalytics v")
    Long getUniqueUsersCount();
    
    @Query("SELECT COUNT(v) FROM VoiceAnalytics v WHERE v.createdAt >= :start")
    Long countRecentCommands(@Param("start") LocalDateTime start);
}