package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.Complaint;
import com.vcsm.repository.UserRepository;
import com.vcsm.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserProfileBuilder {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    /**
     * Build complete user profile
     */
    public UserProfile buildProfile(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        UserProfile profile = new UserProfile(user);
        
        // Add complaint history
        List<Complaint> complaints = complaintRepository.findByResidentUsernameOrderByCreatedAtDesc(user.getEmail());
        profile.setComplaintHistory(complaints);
        
        // Calculate preferences
        Map<String, Long> categoryPreferences = complaints.stream()
            .filter(c -> c.getCategory() != null)
            .collect(Collectors.groupingBy(
                c -> c.getCategory().toString(),
                Collectors.counting()
            ));
        profile.setCategoryPreferences(categoryPreferences);
        
        // Calculate activity patterns
        profile.setActiveHours(getActiveHours(complaints));
        profile.setPreferredChannels(getPreferredChannels(user));
        profile.setEngagementScore(calculateEngagementScore(complaints));
        
        return profile;
    }

    private Map<String, Integer> getActiveHours(List<Complaint> complaints) {
        Map<String, Integer> hours = new HashMap<>();
        for (Complaint c : complaints) {
            if (c.getCreatedAt() != null) {
                String hour = String.format("%02d:00", c.getCreatedAt().getHour());
                hours.put(hour, hours.getOrDefault(hour, 0) + 1);
            }
        }
        return hours;
    }

    private List<String> getPreferredChannels(User user) {
        List<String> channels = new ArrayList<>();
        channels.add("Web");
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            channels.add("SMS");
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            channels.add("Email");
        }
        return channels;
    }

    private int calculateEngagementScore(List<Complaint> complaints) {
        if (complaints.isEmpty()) return 0;
        int score = Math.min(100, complaints.size() * 5);
        long resolved = complaints.stream()
            .filter(c -> c.getStatus() == Complaint.ComplaintStatus.RESOLVED)
            .count();
        if (resolved > 0) {
            score += Math.min(20, (int) resolved * 2);
        }
        return Math.min(100, score);
    }

    public static class UserProfile {
        private final Long userId;
        private final String name;
        private final String email;
        private List<Complaint> complaintHistory;
        private Map<String, Long> categoryPreferences;
        private Map<String, Integer> activeHours;
        private List<String> preferredChannels;
        private int engagementScore;

        public UserProfile(User user) {
            this.userId = user.getId();
            this.name = user.getName();
            this.email = user.getEmail();
        }

        // Getters and Setters
        public Long getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public List<Complaint> getComplaintHistory() { return complaintHistory; }
        public void setComplaintHistory(List<Complaint> complaintHistory) { this.complaintHistory = complaintHistory; }
        public Map<String, Long> getCategoryPreferences() { return categoryPreferences; }
        public void setCategoryPreferences(Map<String, Long> categoryPreferences) { this.categoryPreferences = categoryPreferences; }
        public Map<String, Integer> getActiveHours() { return activeHours; }
        public void setActiveHours(Map<String, Integer> activeHours) { this.activeHours = activeHours; }
        public List<String> getPreferredChannels() { return preferredChannels; }
        public void setPreferredChannels(List<String> preferredChannels) { this.preferredChannels = preferredChannels; }
        public int getEngagementScore() { return engagementScore; }
        public void setEngagementScore(int engagementScore) { this.engagementScore = engagementScore; }
    }
}