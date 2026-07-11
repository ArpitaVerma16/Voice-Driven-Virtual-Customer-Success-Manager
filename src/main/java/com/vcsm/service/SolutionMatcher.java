package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@lombok.RequiredArgsConstructor
public class SolutionMatcher {

    private final ComplaintRepository complaintRepository;

    // Pre-defined solution templates
    private final Map<String, List<SolutionTemplate>> solutionTemplates = new HashMap<>();

    public SolutionMatcher() {
        loadTemplates();
    }

    private void loadTemplates() {
        // NOISE templates
        solutionTemplates.put("NOISE", Arrays.asList(
            new SolutionTemplate(
                "Noise Complaint - First Response",
                "Thank you for reporting the noise issue. We have sent a notification to the concerned resident. Please allow 2-4 hours for resolution.",
                "Send warning notification, Monitor for compliance"
            ),
            new SolutionTemplate(
                "Noise Complaint - Escalation",
                "We have escalated your noise complaint to the security team. They will visit the location within 1 hour.",
                "Security team dispatched, Escalation logged"
            ),
            new SolutionTemplate(
                "Noise Complaint - Resolution",
                "The noise issue has been resolved. The resident has been advised to maintain quiet hours. Please confirm if this is acceptable.",
                "Resident warned, Follow-up required"
            )
        ));

        // MAINTENANCE templates
        solutionTemplates.put("MAINTENANCE", Arrays.asList(
            new SolutionTemplate(
                "Maintenance - Initial",
                "We have received your maintenance request. A technician has been assigned and will contact you within 4 hours.",
                "Technician assigned, Service ticket created"
            ),
            new SolutionTemplate(
                "Maintenance - Completion",
                "The maintenance work has been completed. Please confirm if the issue is resolved to your satisfaction.",
                "Work completed, Feedback requested"
            )
        ));

        // SECURITY templates
        solutionTemplates.put("SECURITY", Arrays.asList(
            new SolutionTemplate(
                "Security - Immediate",
                "This has been escalated to the security team immediately. Security personnel have been dispatched.",
                "Security dispatched, Incident logged"
            ),
            new SolutionTemplate(
                "Security - Follow-up",
                "Security team has investigated the issue. CCTV footage is being reviewed. Will update you within 4 hours.",
                "CCTV review in progress"
            )
        ));
    }

    /**
     * Find matching solutions for a complaint
     */
    public List<SolutionMatch> findMatchingSolutions(Complaint complaint) {
        String category = complaint.getCategory() != null ? complaint.getCategory().toString() : "OTHER";
        List<SolutionTemplate> templates = solutionTemplates.getOrDefault(category, new ArrayList<>());

        // Find similar complaints
        List<Complaint> similar = findSimilarComplaints(complaint);

        // Build matches
        List<SolutionMatch> matches = new ArrayList<>();
        for (SolutionTemplate template : templates) {
            double relevance = calculateRelevance(complaint, template, similar);
            matches.add(new SolutionMatch(template, relevance));
        }

        // Sort by relevance
        matches.sort((a, b) -> Double.compare(b.getRelevance(), a.getRelevance()));
        return matches;
    }

    private List<Complaint> findSimilarComplaints(Complaint complaint) {
        return complaintRepository.findAll().stream()
            .filter(c -> !c.getId().equals(complaint.getId()))
            .filter(c -> c.getCategory() == complaint.getCategory())
            .filter(c -> c.getStatus() == Complaint.ComplaintStatus.RESOLVED)
            .limit(5)
            .collect(Collectors.toList());
    }

    private double calculateRelevance(Complaint complaint, SolutionTemplate template, List<Complaint> similar) {
        double score = 0.5; // Base score
        
        // Boost if similar complaints exist
        if (!similar.isEmpty()) {
            score += 0.2;
        }
        
        // Boost if priority high
        if ("HIGH".equals(complaint.getPriority()) || "CRITICAL".equals(complaint.getPriority())) {
            score += 0.2;
        }
        
        return Math.min(1.0, score);
    }

    public static class SolutionTemplate {
        private final String title;
        private final String content;
        private final String actions;

        public SolutionTemplate(String title, String content, String actions) {
            this.title = title;
            this.content = content;
            this.actions = actions;
        }

        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getActions() { return actions; }
    }

    public static class SolutionMatch {
        private final SolutionTemplate template;
        private final double relevance;

        public SolutionMatch(SolutionTemplate template, double relevance) {
            this.template = template;
            this.relevance = relevance;
        }

        public SolutionTemplate getTemplate() { return template; }
        public double getRelevance() { return relevance; }
    }
}