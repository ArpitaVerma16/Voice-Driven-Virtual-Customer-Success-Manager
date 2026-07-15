package com.vcsm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@lombok.RequiredArgsConstructor
public class PersonalizationEngine {

    private final UserProfileBuilder userProfileBuilder;

    private final RecommendationService recommendationService;

    /**
     * Get complete personalized experience for user
     */
    public PersonalizedExperience getPersonalizedExperience(Long userId) {
        UserProfileBuilder.UserProfile profile = userProfileBuilder.buildProfile(userId);
        if (profile == null) return null;

        RecommendationService.Recommendations recommendations = 
            recommendationService.getRecommendations(userId);

        // Generate personalized settings
        Map<String, Object> settings = generateSettings(profile);

        // Generate dashboard layout
        List<String> dashboardLayout = generateDashboardLayout(profile);

        return new PersonalizedExperience(
            profile,
            recommendations,
            settings,
            dashboardLayout
        );
    }

    private Map<String, Object> generateSettings(UserProfileBuilder.UserProfile profile) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("theme", profile.getEngagementScore() > 50 ? "dark" : "light");
        settings.put("language", "en");
        settings.put("notifications", profile.getPreferredChannels());
        settings.put("dashboardStyle", profile.getEngagementScore() > 70 ? "compact" : "detailed");
        return settings;
    }

    private List<String> generateDashboardLayout(UserProfileBuilder.UserProfile profile) {
        List<String> layout = new ArrayList<>();
        
        if (profile.getEngagementScore() > 70) {
            layout.add("analytics");
            layout.add("recent_complaints");
            layout.add("recommendations");
            layout.add("quick_actions");
        } else if (profile.getEngagementScore() > 30) {
            layout.add("recent_complaints");
            layout.add("recommendations");
            layout.add("quick_actions");
        } else {
            layout.add("quick_actions");
            layout.add("getting_started");
        }
        
        return layout;
    }

    public static class PersonalizedExperience {
        private final UserProfileBuilder.UserProfile profile;
        private final RecommendationService.Recommendations recommendations;
        private final Map<String, Object> settings;
        private final List<String> dashboardLayout;

        public PersonalizedExperience(UserProfileBuilder.UserProfile profile, 
                                      RecommendationService.Recommendations recommendations,
                                      Map<String, Object> settings,
                                      List<String> dashboardLayout) {
            this.profile = profile;
            this.recommendations = recommendations;
            this.settings = settings;
            this.dashboardLayout = dashboardLayout;
        }

        public UserProfileBuilder.UserProfile getProfile() { return profile; }
        public RecommendationService.Recommendations getRecommendations() { return recommendations; }
        public Map<String, Object> getSettings() { return settings; }
        public List<String> getDashboardLayout() { return dashboardLayout; }
    }
}