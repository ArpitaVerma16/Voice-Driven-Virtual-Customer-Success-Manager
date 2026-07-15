package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.Event;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@lombok.RequiredArgsConstructor
public class RecommendationService {

    private final ComplaintRepository complaintRepository;

    private final EventRepository eventRepository;

    /**
     * Get personalized recommendations for user
     */
    public Recommendations getRecommendations(Long userId) {
        List<Complaint> userComplaints = complaintRepository.findByResidentUsernameOrderByCreatedAtDesc("user" + userId);
        
        // Get top categories
        Map<String, Long> categoryCounts = userComplaints.stream()
            .filter(c -> c.getCategory() != null)
            .collect(Collectors.groupingBy(
                c -> c.getCategory().toString(),
                Collectors.counting()
            ));

        // Recommend similar complaints
        List<Complaint> similarComplaints = getSimilarComplaints(userComplaints);

        // Recommend events based on preferences
        List<Event> recommendedEvents = getRecommendedEvents(categoryCounts);

        // Generate insights
        List<String> insights = generateInsights(userComplaints);

        return new Recommendations(similarComplaints, recommendedEvents, insights);
    }

    private List<Complaint> getSimilarComplaints(List<Complaint> userComplaints) {
        if (userComplaints.isEmpty()) return new ArrayList<>();
        
        Set<String> keywords = userComplaints.stream()
            .flatMap(c -> Arrays.stream(c.getDescription().toLowerCase().split(" ")))
            .filter(w -> w.length() > 3)
            .collect(Collectors.toSet());

        return complaintRepository.findAll().stream()
            .filter(c -> !userComplaints.contains(c))
            .filter(c -> {
                String desc = c.getDescription().toLowerCase();
                return keywords.stream().anyMatch(desc::contains);
            })
            .limit(5)
            .collect(Collectors.toList());
    }

    private List<Event> getRecommendedEvents(Map<String, Long> categoryCounts) {
        List<Event> allEvents = eventRepository.findAll();
        
        if (categoryCounts.isEmpty() || allEvents.isEmpty()) {
            return allEvents.stream().limit(3).collect(Collectors.toList());
        }

        // Score events based on category preferences
        Map<Event, Double> scores = new HashMap<>();
        for (Event event : allEvents) {
            double score = 0;
            if (event.getCategory() != null) {
                score = categoryCounts.getOrDefault(event.getCategory().toString(), 0L).doubleValue();
            }
            scores.put(event, score);
        }

        return scores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private List<String> generateInsights(List<Complaint> complaints) {
        List<String> insights = new ArrayList<>();
        
        if (complaints.isEmpty()) {
            insights.add("👋 You're new! Try filing a complaint to get started.");
            return insights;
        }

        long resolved = complaints.stream()
            .filter(c -> c.getStatus() == Complaint.ComplaintStatus.RESOLVED)
            .count();

        if (resolved == complaints.size()) {
            insights.add("✅ All your complaints are resolved! Great job!");
        } else {
            insights.add("📋 You have " + (complaints.size() - resolved) + " unresolved complaints.");
        }

        if (complaints.size() > 5) {
            insights.add("📊 You're a power user! Thank you for your engagement.");
        }

        return insights;
    }

    public static class Recommendations {
        private final List<Complaint> similarComplaints;
        private final List<Event> recommendedEvents;
        private final List<String> insights;

        public Recommendations(List<Complaint> similarComplaints, List<Event> recommendedEvents, List<String> insights) {
            this.similarComplaints = similarComplaints;
            this.recommendedEvents = recommendedEvents;
            this.insights = insights;
        }

        public List<Complaint> getSimilarComplaints() { return similarComplaints; }
        public List<Event> getRecommendedEvents() { return recommendedEvents; }
        public List<String> getInsights() { return insights; }
    }
}