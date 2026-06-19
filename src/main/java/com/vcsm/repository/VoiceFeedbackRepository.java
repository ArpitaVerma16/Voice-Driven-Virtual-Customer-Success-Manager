package com.vcsm.repository;

import com.vcsm.model.VoiceFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoiceFeedbackRepository extends JpaRepository<VoiceFeedback, Long> {
    
    List<VoiceFeedback> findByFeedback(String feedback);
    
    @Query("SELECT COUNT(v) FROM VoiceFeedback v WHERE v.feedback = 'UP'")
    long countUpvotes();
    
    @Query("SELECT COUNT(v) FROM VoiceFeedback v WHERE v.feedback = 'DOWN'")
    long countDownvotes();
}