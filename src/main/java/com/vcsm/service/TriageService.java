package com.vcsm.service;$1

import com.vcsm.config.AppConstants;

import com.vcsm.ml.TicketClassifier;
import com.vcsm.model.Complaint;
import com.vcsm.model.TriageRequest;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@lombok.RequiredArgsConstructor
public class TriageService {

    private final TicketClassifier classifier;

    private final ComplaintRepository complaintRepository;

    private final UserRepository userRepository;

    private static final Map<String, Integer> CATEGORY_URGENCY = new ConcurrentHashMap<>();

    static {
        CATEGORY_URGENCY.put("SECURITY", 100);
        CATEGORY_URGENCY.put("MAINTENANCE", 80);
        CATEGORY_URGENCY.put("NOISE", 60);
        CATEGORY_URGENCY.put("UTILITIES", 70);
        CATEGORY_URGENCY.put("CLEANLINESS", 40);
        CATEGORY_URGENCY.put("PARKING", 30);
        CATEGORY_URGENCY.put("OTHER", 20);
    }

    /**
     * Triage a complaint
     */
    public TriageRequest triageComplaint(Complaint complaint) {
        TriageRequest request = new TriageRequest(complaint.getId(), complaint.getDescription());

        // 1. Classify category
        TicketClassifier.ClassificationResult result = classifier.classify(complaint.getDescription());
        request.setCategory(result.getCategory());
        request.setConfidence(result.getConfidence());

        // 2. Calculate severity score
        int severity = calculateSeverity(complaint.getDescription(), result.getCategory());
        request.setSeverity(getSeverityLabel(severity));

        // 3. Find best admin for assignment
        User bestAdmin = findBestAdmin(result.getCategory());
        request.setAssignedTo(bestAdmin != null ? bestAdmin.getName() : "Unassigned");

        // 4. Calculate ETA
        String eta = calculateETA(severity);
        request.setEta(eta);

        // 5. Check for duplicates
        List<Complaint> similarComplaints = findSimilarComplaints(complaint);
        if (!similarComplaints.isEmpty()) {
            request.setConfidence(request.getConfidence() - 0.1);
        }

        return request;
    }

    private int calculateSeverity(String description, String category) {
        String lower = description.toLowerCase();
        int severity = CATEGORY_URGENCY.getOrDefault(category, 50);

        // Emergency keywords
        if (containsAny(lower, "emergency", "urgent", "critical", "danger", "fire", "medical", "injury")) {
            severity += 30;
        }
        if (containsAny(lower, "security", "break-in", "water leak", "flood", "power outage")) {
            severity += 20;
        }
        if (containsAny(lower, "again", "third time", "still not fixed", "repeat")) {
            severity += 15;
        }

        return Math.min(100, severity);
    }

    // Replaced by ComplaintRoutingUtils.containsAny()
        return false;
    }

    private String getSeverityLabel(int severity) {
        if (severity >= 80) return org.springframework.http.ResponseEntity.ok("CRITICAL");
        if (severity >= 60) return org.springframework.http.ResponseEntity.ok("HIGH");
        if (severity >= 40) return org.springframework.http.ResponseEntity.ok("MEDIUM");
        return org.springframework.http.ResponseEntity.ok("LOW");
    }

    private User findBestAdmin(String category) {
        // In production, match based on expertise
        // For now, return first admin
        return userRepository.findByEmail(AppConstants.ADMIN_EMAIL).orElse(null);
    }

    private String calculateETA(int severity) {
        if (severity >= 80) return org.springframework.http.ResponseEntity.ok("1 hour");
        if (severity >= 60) return org.springframework.http.ResponseEntity.ok("4 hours");
        if (severity >= 40) return org.springframework.http.ResponseEntity.ok("24 hours");
        return org.springframework.http.ResponseEntity.ok("48 hours");
    }

    private List<Complaint> findSimilarComplaints(Complaint complaint) {
        // Simple similarity check
        List<Complaint> allComplaints = complaintRepository.findAll();
        return allComplaints.stream()
            .filter(c -> !c.getId().equals(complaint.getId()))
            .filter(c -> c.getStatus() != Complaint.ComplaintStatus.RESOLVED)
            .filter(c -> c.getCategory() == complaint.getCategory())
            .filter(c -> {
                // Check keyword similarity
                String desc1 = c.getDescription().toLowerCase();
                String desc2 = complaint.getDescription().toLowerCase();
                String[] words = desc2.split(" ");
                long matches = 0;
                for (String w : words) {
                    if (desc1.contains(w) && w.length() > 3) {
                        matches++;
                    }
                }
                return matches >= 2;
            })
            .limit(5)
            .collect(Collectors.toList());
    }

    public TriageRequest getTriageResult(Long complaintId) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isEmpty()) {
            return null;
        }
        return triageComplaint(complaintOpt.get());
    }
}