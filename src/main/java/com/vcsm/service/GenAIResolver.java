package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GenAIResolver {

    @Autowired
    private SolutionMatcher solutionMatcher;

    @Autowired
    private ResponseGenerator responseGenerator;

    @Autowired
    private ComplaintRepository complaintRepository;

    /**
     * Resolve complaint using GenAI
     */
    public ResolutionResult resolveComplaint(Complaint complaint) {
        // Find matching solutions
        List<SolutionMatcher.SolutionMatch> matches = solutionMatcher.findMatchingSolutions(complaint);

        // Generate response
        String tone = determineTone(complaint);
        String language = determineLanguage(complaint);
        
        ResponseGenerator.ResolutionResponse response = 
            responseGenerator.generateResponse(complaint, tone, language);

        // Create result
        ResolutionResult result = new ResolutionResult(
            complaint.getId(),
            response,
            matches,
            generateFeedback(matches, response)
        );

        // Update complaint with resolution (if confidence high)
        if (response.getConfidence() > 0.8) {
            complaint.setResolutionNotes(response.getFullResponse());
            complaintRepository.save(complaint);
        }

        return result;
    }

    private String determineTone(Complaint complaint) {
        String desc = complaint.getDescription().toLowerCase();
        if (desc.contains("emergency") || desc.contains("urgent") || desc.contains("critical")) {
            return "urgent";
        }
        if (desc.contains("frustrated") || desc.contains("unhappy") || desc.contains("disappointed")) {
            return "empathy";
        }
        return "standard";
    }

    private String determineLanguage(Complaint complaint) {
        // Simple language detection
        String desc = complaint.getDescription();
        if (desc.matches(".*[\\u0900-\\u097F].*")) {
            return "hi";
        }
        return "en";
    }

    private String generateFeedback(List<SolutionMatcher.SolutionMatch> matches, 
                                    ResponseGenerator.ResolutionResponse response) {
        StringBuilder feedback = new StringBuilder();
        feedback.append("📊 Resolution Analysis:\n");
        feedback.append("- Confidence: " + String.format("%.0f%%", response.getConfidence() * 100) + "\n");
        feedback.append("- Solutions Found: " + matches.size() + "\n");
        feedback.append("- Best Match: " + (matches.isEmpty() ? "None" : matches.get(0).getTemplate().getTitle()) + "\n");
        feedback.append("\n💡 Recommendation: ");
        
        if (response.getConfidence() > 0.8) {
            feedback.append("Auto-resolve with high confidence");
        } else if (response.getConfidence() > 0.5) {
            feedback.append("Review and approve before sending");
        } else {
            feedback.append("Manual resolution required");
        }
        
        return feedback.toString();
    }

    public static class ResolutionResult {
        private final Long complaintId;
        private final ResponseGenerator.ResolutionResponse response;
        private final List<SolutionMatcher.SolutionMatch> matches;
        private final String feedback;

        public ResolutionResult(Long complaintId, ResponseGenerator.ResolutionResponse response,
                                List<SolutionMatcher.SolutionMatch> matches, String feedback) {
            this.complaintId = complaintId;
            this.response = response;
            this.matches = matches;
            this.feedback = feedback;
        }

        public Long getComplaintId() { return complaintId; }
        public ResponseGenerator.ResolutionResponse getResponse() { return response; }
        public List<SolutionMatcher.SolutionMatch> getMatches() { return matches; }
        public String getFeedback() { return feedback; }
    }
}