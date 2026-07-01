package com.vcsm.service;

import com.vcsm.model.Complaint;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ResponseGenerator {

    private final Random random = new Random();

    /**
     * Generate personalized resolution response
     */
    public ResolutionResponse generateResponse(Complaint complaint, String tone, String language) {
        String category = complaint.getCategory() != null ? complaint.getCategory().toString() : "OTHER";
        
        // Generate empathy statement
        String empathy = generateEmpathy(tone);
        
        // Generate resolution steps
        List<String> steps = generateSteps(category, complaint);
        
        // Generate action items
        List<String> actions = generateActions(category);
        
        // Generate timeline
        String timeline = generateTimeline(complaint);
        
        // Generate closing
        String closing = generateClosing(tone);

        String fullResponse = String.join("\n\n",
            empathy,
            "Resolution Steps:",
            String.join("\n", steps),
            "\nAction Items:",
            String.join("\n", actions),
            timeline,
            closing
        );

        return new ResolutionResponse(
            fullResponse,
            steps,
            actions,
            timeline,
            generateConfidence(complaint)
        );
    }

    private String generateEmpathy(String tone) {
        switch (tone) {
            case "empathy":
                return "We understand how frustrating this issue can be and we sincerely apologize for the inconvenience caused. We are committed to resolving this as quickly as possible.";
            case "urgent":
                return "We recognize the urgency of this matter and are treating it with the highest priority. Our team is actively working on this.";
            default:
                return "Thank you for bringing this to our attention. We are here to help resolve your issue.";
        }
    }

    private List<String> generateSteps(String category, Complaint complaint) {
        List<String> steps = new ArrayList<>();
        
        switch (category) {
            case "NOISE":
                steps.add("1. Notify the concerned resident about the noise complaint");
                steps.add("2. Send a formal warning if noise continues");
                steps.add("3. Document the complaint for future reference");
                steps.add("4. Follow up with you within 24 hours");
                break;
            case "MAINTENANCE":
                steps.add("1. Assign a technician to assess the issue");
                steps.add("2. Schedule a visit at your convenience");
                steps.add("3. Complete the repair work");
                steps.add("4. Inspect and ensure quality");
                break;
            case "SECURITY":
                steps.add("1. Escalate to security team immediately");
                steps.add("2. Review CCTV footage if available");
                steps.add("3. Take necessary action as per protocol");
                steps.add("4. Update you on the resolution");
                break;
            default:
                steps.add("1. Log the complaint in our system");
                steps.add("2. Assign to the appropriate team");
                steps.add("3. Investigate the issue");
                steps.add("4. Provide resolution and follow-up");
        }
        
        return steps;
    }

    private List<String> generateActions(String category) {
        List<String> actions = new ArrayList<>();
        
        switch (category) {
            case "NOISE":
                actions.add("📞 Contact resident (1 hour)");
                actions.add("📝 Issue warning (2 hours)");
                actions.add("✅ Follow-up (24 hours)");
                break;
            case "MAINTENANCE":
                actions.add("🔧 Assign technician (4 hours)");
                actions.add("📅 Schedule visit (24 hours)");
                actions.add("✅ Complete repair (48 hours)");
                break;
            case "SECURITY":
                actions.add("🚨 Dispatch security (15 minutes)");
                actions.add("📹 Review footage (2 hours)");
                actions.add("📋 File report (4 hours)");
                break;
            default:
                actions.add("📝 Log complaint (1 hour)");
                actions.add("📧 Assign team (4 hours)");
                actions.add("✅ Provide update (24 hours)");
        }
        
        return actions;
    }

    private String generateTimeline(Complaint complaint) {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        String eta = "24 hours";
        
        if (complaint.getPriority() != null) {
            if ("CRITICAL".equals(complaint.getPriority())) eta = "1 hour";
            else if ("HIGH".equals(complaint.getPriority())) eta = "4 hours";
        }
        
        return "📅 Resolution Timeline: " + eta + " (Started: " + date + ")";
    }

    private String generateClosing(String tone) {
        switch (tone) {
            case "empathy":
                return "We appreciate your patience and understanding. Please don't hesitate to reach out if you need any further assistance.";
            case "urgent":
                return "We will keep you updated on the progress. Thank you for your cooperation.";
            default:
                return "Thank you for your trust in us. We are committed to resolving your issue satisfactorily.";
        }
    }

    private double generateConfidence(Complaint complaint) {
        // Higher confidence for simpler issues
        double base = 0.7;
        if (complaint.getPriority() != null) {
            if ("LOW".equals(complaint.getPriority())) base += 0.2;
            else if ("MEDIUM".equals(complaint.getPriority())) base += 0.1;
            else if ("HIGH".equals(complaint.getPriority())) base -= 0.1;
            else if ("CRITICAL".equals(complaint.getPriority())) base -= 0.2;
        }
        return Math.min(0.95, Math.max(0.5, base));
    }

    public static class ResolutionResponse {
        private final String fullResponse;
        private final List<String> steps;
        private final List<String> actions;
        private final String timeline;
        private final double confidence;

        public ResolutionResponse(String fullResponse, List<String> steps, List<String> actions, 
                                  String timeline, double confidence) {
            this.fullResponse = fullResponse;
            this.steps = steps;
            this.actions = actions;
            this.timeline = timeline;
            this.confidence = confidence;
        }

        public String getFullResponse() { return fullResponse; }
        public List<String> getSteps() { return steps; }
        public List<String> getActions() { return actions; }
        public String getTimeline() { return timeline; }
        public double getConfidence() { return confidence; }
    }
}