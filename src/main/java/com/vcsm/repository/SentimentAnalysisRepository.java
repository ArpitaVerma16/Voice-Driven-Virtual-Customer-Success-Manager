package com.vcsm.repository;

import com.vcsm.model.SentimentAnalysis;
import com.vcsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SentimentAnalysisRepository extends JpaRepository<SentimentAnalysis, Long> {
    
    List<SentimentAnalysis> findByUser(User user);
    
    List<SentimentAnalysis> findBySentiment(String sentiment);
    
    List<SentimentAnalysis> findByWasEscalated(boolean wasEscalated);
    
    @Query("SELECT s FROM SentimentAnalysis s WHERE s.createdAt BETWEEN :start AND :end")
    List<SentimentAnalysis> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(s) FROM SentimentAnalysis s WHERE s.sentiment = :sentiment")
    long countBySentiment(@Param("sentiment") String sentiment);
}