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
    
    long countByWasEscalated(boolean wasEscalated);
    
    @Query("SELECT s FROM SentimentAnalysis s WHERE s.createdAt BETWEEN :start AND :end")
    List<SentimentAnalysis> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(s) FROM SentimentAnalysis s WHERE s.sentiment = :sentiment")
    long countBySentiment(@Param("sentiment") String sentiment);

    @Query("SELECT CAST(s.createdAt AS date) as trendDate, " +
           "SUM(CASE WHEN s.sentiment IN ('POSITIVE', 'VERY_POSITIVE') THEN 1 ELSE 0 END) as positiveCount, " +
           "SUM(CASE WHEN s.sentiment IN ('NEGATIVE', 'VERY_NEGATIVE') THEN 1 ELSE 0 END) as negativeCount, " +
           "SUM(CASE WHEN s.sentiment = 'NEUTRAL' THEN 1 ELSE 0 END) as neutralCount " +
           "FROM SentimentAnalysis s " +
           "WHERE s.createdAt >= :startDate " +
           "GROUP BY CAST(s.createdAt AS date) " +
           "ORDER BY trendDate ASC")
    List<Object[]> findDailySentimentTrends(@Param("startDate") LocalDateTime startDate);
}