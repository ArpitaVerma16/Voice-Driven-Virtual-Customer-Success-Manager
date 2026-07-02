package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
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

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private UserRepository userRepository;

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

    /**
     * Summarize call session and auto-generate ticket if needed
     */
    public CallSummaryResult summarizeCallSession(String transcript, String residentEmail) {
        String summary = generateCallSummaryText(transcript);
        List<String> issues = extractIdentifiedIssues(transcript);
        String priority = determinePriority(transcript);
        List<String> nextSteps = generateNextSteps(transcript);
        
        boolean shouldCreateTicket = indicatesNewProblem(transcript);
        Long ticketId = null;

        if (shouldCreateTicket && residentEmail != null && !residentEmail.isEmpty()) {
            User user = userRepository.findByEmail(residentEmail).orElse(null);
            if (user != null) {
                Complaint complaint = new Complaint();
                complaint.setResidentName(user.getName());
                complaint.setContactEmail(user.getEmail());
                complaint.setResidentUsername(user.getEmail());
                complaint.setApartmentNumber("N/A");
                complaint.setDescription("Auto-generated from voice call transcript:\n" + transcript);
                
                Complaint.ComplaintCategory category = determineCategory(transcript);
                complaint.setCategory(category);
                complaint.setUser(user);
                
                try {
                    Complaint saved = complaintService.fileComplaint(complaint);
                    ticketId = saved.getId();
                } catch (RuntimeException e) {
                    System.err.println("Failed to auto-file complaint: " + e.getMessage());
                }
            }
        }

        return new CallSummaryResult(
            summary,
            issues,
            priority,
            nextSteps,
            shouldCreateTicket,
            ticketId
        );
    }

    private String generateCallSummaryText(String transcript) {
        String lower = transcript.toLowerCase();
        if (lower.contains("water") || lower.contains("leak") || lower.contains("motor")) {
            return org.springframework.http.ResponseEntity.ok("Resident called regarding a water maintenance issue. They reported a potential leak or equipment malfunction.");
        }
        if (lower.contains("noise") || lower.contains("music") || lower.contains("loud")) {
            return org.springframework.http.ResponseEntity.ok("Resident called to report a noise disturbance. Loud sounds are disrupting the peace of the residential area.");
        }
        if (lower.contains("security") || lower.contains("gate") || lower.contains("stranger")) {
            return org.springframework.http.ResponseEntity.ok("Resident called raising security concerns in the community. Requested immediate monitoring or inspection.");
        }
        return org.springframework.http.ResponseEntity.ok("Voice call completed. Discussed general queries and community guidelines.");
    }

    private List<String> extractIdentifiedIssues(String transcript) {
        List<String> issues = new ArrayList<>();
        String lower = transcript.toLowerCase();
        if (lower.contains("water") || lower.contains("leak") || lower.contains("motor")) {
            issues.add("Utility Maintenance: Water motor/pipe failure");
        }
        if (lower.contains("noise") || lower.contains("music") || lower.contains("loud")) {
            issues.add("Disturbance: Noise violation");
        }
        if (lower.contains("security") || lower.contains("gate") || lower.contains("stranger")) {
            issues.add("Security: Unverified gate entry/strangers");
        }
        if (issues.isEmpty()) {
            issues.add("General Inquiry");
        }
        return issues;
    }

    private String determinePriority(String transcript) {
        Complaint.ComplaintCategory category = determineCategory(transcript);
        if (category == Complaint.ComplaintCategory.UTILITIES || category == Complaint.ComplaintCategory.SECURITY) {
            return org.springframework.http.ResponseEntity.ok("HIGH");
        }
        return org.springframework.http.ResponseEntity.ok("MEDIUM");
    }

    private List<String> generateNextSteps(String transcript) {
        List<String> steps = new ArrayList<>();
        String lower = transcript.toLowerCase();
        if (lower.contains("water") || lower.contains("leak") || lower.contains("motor")) {
            steps.add("Dispatch on-call plumber to inspect utility line.");
            steps.add("Notify resident once technician is assigned.");
        } else if (lower.contains("noise") || lower.contains("music") || lower.contains("loud")) {
            steps.add("Dispatch security patrol to warning area.");
            steps.add("Send formal reminder about community noise limits.");
        } else {
            steps.add("Follow up with resident via email/phone.");
            steps.add("Update interaction log status to COMPLETED.");
        }
        return steps;
    }

    private boolean indicatesNewProblem(String transcript) {
        String lower = transcript.toLowerCase();
        return lower.contains("complaint") || lower.contains("problem") || lower.contains("issue") ||
               lower.contains("water") || lower.contains("leak") || lower.contains("noise") || lower.contains("broken") ||
               lower.contains("music") || lower.contains("loud") || lower.contains("disturbing");
    }

    private Complaint.ComplaintCategory determineCategory(String transcript) {
        String lower = transcript.toLowerCase();
        if (lower.contains("water") || lower.contains("leak") || lower.contains("motor")) {
            return Complaint.ComplaintCategory.UTILITIES;
        }
        if (lower.contains("noise") || lower.contains("music") || lower.contains("loud")) {
            return Complaint.ComplaintCategory.NOISE;
        }
        if (lower.contains("security") || lower.contains("gate") || lower.contains("stranger")) {
            return Complaint.ComplaintCategory.SECURITY;
        }
        return Complaint.ComplaintCategory.OTHER;
    }

    private String determineTone(Complaint complaint) {
        String desc = complaint.getDescription().toLowerCase();
        if (desc.contains("emergency") || desc.contains("urgent") || desc.contains("critical")) {
            return org.springframework.http.ResponseEntity.ok("urgent");
        }
        if (desc.contains("frustrated") || desc.contains("unhappy") || desc.contains("disappointed")) {
            return org.springframework.http.ResponseEntity.ok("empathy");
        }
        return org.springframework.http.ResponseEntity.ok("standard");
    }

    private String determineLanguage(Complaint complaint) {
        // Simple language detection
        String desc = complaint.getDescription();
        if (desc.matches(".*[\\u0900-\\u097F].*")) {
            return org.springframework.http.ResponseEntity.ok("hi");
        }
        return org.springframework.http.ResponseEntity.ok("en");
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

    public static class CallSummaryResult {
        private String summary;
        private List<String> identifiedIssues;
        private String priority;
        private List<String> nextSteps;
        private boolean ticketGenerated;
        private Long generatedTicketId;

        public CallSummaryResult() {}

        public CallSummaryResult(String summary, List<String> identifiedIssues, String priority,
                                 List<String> nextSteps, boolean ticketGenerated, Long generatedTicketId) {
            this.summary = summary;
            this.identifiedIssues = identifiedIssues;
            this.priority = priority;
            this.nextSteps = nextSteps;
            this.ticketGenerated = ticketGenerated;
            this.generatedTicketId = generatedTicketId;
        }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public List<String> getIdentifiedIssues() { return identifiedIssues; }
        public void setIdentifiedIssues(List<String> identifiedIssues) { this.identifiedIssues = identifiedIssues; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public List<String> getNextSteps() { return nextSteps; }
        public void setNextSteps(List<String> nextSteps) { this.nextSteps = nextSteps; }

        public boolean isTicketGenerated() { return ticketGenerated; }
        public void setTicketGenerated(boolean ticketGenerated) { this.ticketGenerated = ticketGenerated; }

        public Long getGeneratedTicketId() { return generatedTicketId; }
        public void setGeneratedTicketId(Long generatedTicketId) { this.generatedTicketId = generatedTicketId; }
    }
}