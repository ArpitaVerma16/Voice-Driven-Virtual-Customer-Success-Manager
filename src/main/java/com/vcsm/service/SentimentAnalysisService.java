package com.vcsm.service;

import com.vcsm.model.*;
import com.vcsm.repository.*;
import com.vcsm.utils.SentimentClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SentimentAnalysisService {
    
    @Autowired
    private SentimentClassifier sentimentClassifier;
    
    @Autowired
    private SentimentAnalysisRepository sentimentRepository;
    
    @Autowired
    private EscalatedCaseRepository escalatedRepository;
    
    @Autowired
    private ComplaintRepository complaintRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public SentimentAnalysis analyzeAndProcess(Long userId, String transcribedText) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        
        // Analyze sentiment
        SentimentClassifier.SentimentResult result = sentimentClassifier.analyze(transcribedText);
        
        // Create sentiment record
        SentimentAnalysis sentimentAnalysis = new SentimentAnalysis(
            user, result.getSentiment(), result.getConfidence(), transcribedText
        );
        
        // Check if needs escalation
        if (sentimentClassifier.shouldEscalate(result.getSentiment())) {
            // Create HIGH priority complaint
            Complaint complaint = new Complaint();
            complaint.setUser(user);
            if (user != null) {
                complaint.setResidentName(user.getName());
                complaint.setResidentUsername(user.getEmail());
                complaint.setContactEmail(user.getEmail());
            } else {
                complaint.setResidentName("System Auto-Escalation");
            }
            complaint.setDescription("[AUTO-ESCALATED] " + transcribedText);
            complaint.setCategory(Complaint.ComplaintCategory.OTHER);
            complaint.setStatus(Complaint.ComplaintStatus.OPEN);
            complaint.setPriority("HIGH");
            complaint.setCreatedAt(LocalDateTime.now());
            complaintRepository.save(complaint);
            
            sentimentAnalysis.setComplaint(complaint);
            sentimentAnalysis.setWasEscalated(true);
            
            // Create escalated case record
            EscalatedCase escalatedCase = new EscalatedCase(sentimentAnalysis);
            escalatedRepository.save(escalatedCase);
        }
        
        return sentimentRepository.save(sentimentAnalysis);
    }
    
    public Map<String, Object> getSentimentStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long total = sentimentRepository.count();
        long positive = sentimentRepository.countBySentiment("POSITIVE") + 
                        sentimentRepository.countBySentiment("VERY_POSITIVE");
        long negative = sentimentRepository.countBySentiment("NEGATIVE") + 
                        sentimentRepository.countBySentiment("VERY_NEGATIVE");
        long neutral = sentimentRepository.countBySentiment("NEUTRAL");
        long escalated = sentimentRepository.countByWasEscalated(true);
        
        double csatScore = total > 0 ? (positive * 100.0 / total) : 0;
        
        stats.put("total", total);
        stats.put("positive", positive);
        stats.put("negative", negative);
        stats.put("neutral", neutral);
        stats.put("escalated", escalated);
        stats.put("csatScore", Math.round(csatScore));
        
        return stats;
    }
    
    public List<SentimentAnalysis> getRecentAnalyses(int limit) {
        return sentimentRepository.findAll(
            PageRequest.of(0, limit, Sort.by("createdAt").descending())
        ).getContent();
    }
    
    public List<EscalatedCase> getPendingEscalations() {
        return escalatedRepository.findByResolved(false);
    }
}