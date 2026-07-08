package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.SentimentAnalysis;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.SentimentAnalysisRepository;
import com.vcsm.repository.UserRepository;
import com.vcsm.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@lombok.RequiredArgsConstructor
public class PredictiveDissatisfactionTest {

    private final MockMvc mockMvc;

    private final PredictionService predictionService;

    private final UserRepository userRepository;

    private final SentimentAnalysisRepository sentimentAnalysisRepository;

    private final ComplaintRepository complaintRepository;

    private final com.vcsm.security.repo.AppUserRepository appUserRepository;

    private final com.vcsm.security.service.CustomUserDetailsService customUserDetailsService;

    private final JwtService jwtService;

    private final EntityManager entityManager;

    private User testUser;

    @BeforeEach
    public void setup() {
        // Clear repositories to prevent contamination
        sentimentAnalysisRepository.deleteAll();
        complaintRepository.deleteAll();
        userRepository.deleteAll();
        appUserRepository.deleteAll();

        testUser = new User();
        testUser.setName("John Churn");
        testUser.setEmail("john.churn@example.com");
        testUser.setVoiceEnrolled(true);
        testUser = userRepository.save(testUser);
    }

    private String getAuthHeader(String username, com.vcsm.security.model.UserRole role) {
        // Find or create AppUser
        com.vcsm.security.model.AppUser appUser = appUserRepository.findByUsername(username).orElse(null);
        if (appUser == null) {
            appUser = new com.vcsm.security.model.AppUser();
            appUser.setUsername(username);
            appUser.setPasswordHash("hashedpassword");
            appUser.addRole(role);
            appUserRepository.save(appUser);
        }

        org.springframework.security.core.userdetails.UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        String jwtToken = jwtService.generateToken(userDetails);
        return "Bearer " + jwtToken;
    }

    @Test
    public void testSentimentScoringPoints() {
        // 0 negative records -> CDI score is 0
        double initialCDI = predictionService.calculateCustomerDissatisfactionIndex(testUser);
        assertEquals(0.0, initialCDI);

        // Add 1 negative sentiment record
        SentimentAnalysis s1 = new SentimentAnalysis(testUser, "NEGATIVE", 0.85, "I am unhappy");
        sentimentAnalysisRepository.save(s1);

        double score1 = predictionService.calculateCustomerDissatisfactionIndex(testUser);
        assertEquals(15.0, score1);

        // Add 2nd negative sentiment record
        SentimentAnalysis s2 = new SentimentAnalysis(testUser, "VERY_NEGATIVE", 0.90, "This is terrible");
        sentimentAnalysisRepository.save(s2);

        double score2 = predictionService.calculateCustomerDissatisfactionIndex(testUser);
        assertEquals(30.0, score2);

        // Add 3rd negative sentiment record
        SentimentAnalysis s3 = new SentimentAnalysis(testUser, "NEGATIVE", 0.70, "Still bad");
        sentimentAnalysisRepository.save(s3);

        double score3 = predictionService.calculateCustomerDissatisfactionIndex(testUser);
        assertEquals(40.0, score3);
    }

    @Test
    public void testResolutionDelayPoints() {
        // Add a long-pending unresolved complaint (>3 days pending)
        Complaint pendingComplaint = new Complaint();
        pendingComplaint.setResidentName(testUser.getName());
        pendingComplaint.setResidentUsername(testUser.getEmail());
        pendingComplaint.setDescription("Pending maintenance issue");
        pendingComplaint.setStatus(Complaint.ComplaintStatus.OPEN);
        pendingComplaint.setCategory(Complaint.ComplaintCategory.MAINTENANCE);
        pendingComplaint.setContactEmail(testUser.getEmail());
        pendingComplaint.setUser(testUser);
        pendingComplaint = complaintRepository.save(pendingComplaint);

        // Backdate the complaint to 4 days ago via JPQL to bypass @PrePersist callback
        entityManager.createQuery("UPDATE Complaint c SET c.createdAt = :date WHERE c.id = :id")
                .setParameter("date", LocalDateTime.now().minusDays(4))
                .setParameter("id", pendingComplaint.getId())
                .executeUpdate();

        entityManager.clear(); // Clear entity manager to force reload

        double pendingScore = predictionService.calculateCustomerDissatisfactionIndex(testUser);
        // 15 points for long-pending, and 0 for resolved (unresolved count is 1, which adds 0 points to section 3)
        assertEquals(15.0, pendingScore);

        // Resolve the complaint, making its resolution time slow (4 days)
        pendingComplaint = complaintRepository.findById(pendingComplaint.getId()).orElseThrow();
        pendingComplaint.setStatus(Complaint.ComplaintStatus.RESOLVED);
        pendingComplaint.setUpdatedAt(LocalDateTime.now());
        complaintRepository.save(pendingComplaint);

        double resolvedScore = predictionService.calculateCustomerDissatisfactionIndex(testUser);
        // Resolution delay: avgResDays is 4.0 (>2.0 adds 5 points).
        // Since it's now resolved, unresolved count is 0, long-pending count is 0.
        // Cap is 40. Score is 5.0.
        assertEquals(5.0, resolvedScore);
    }

    @Test
    public void testInteractionAndUnresolvedCountPoints() {
        // Add 4 unresolved complaints (should add 20 points)
        for (int i = 0; i < 4; i++) {
            Complaint c = new Complaint();
            c.setResidentName(testUser.getName());
            c.setResidentUsername(testUser.getEmail());
            c.setDescription("Unresolved complaint " + i);
            c.setStatus(Complaint.ComplaintStatus.OPEN);
            c.setCategory(Complaint.ComplaintCategory.OTHER);
            c.setContactEmail(testUser.getEmail());
            c.setUser(testUser);
            complaintRepository.save(c);
        }

        double score = predictionService.calculateCustomerDissatisfactionIndex(testUser);
        // 4 unresolved complaints -> Section 3 adds 20.0 points.
        // Section 2 (resolution delay): complaints are fresh (0 days old), so 0 points.
        // Section 1 (sentiment): 0 points.
        assertEquals(20.0, score);
    }

    @Test
    public void testWeeklyAnalysisExecution() {
        // Build a highly dissatisfied user (CDI >= 75)
        // 3 negative sentiment records (+40 points)
        for (int i = 0; i < 3; i++) {
            sentimentAnalysisRepository.save(new SentimentAnalysis(testUser, "NEGATIVE", 0.9, "Bad"));
        }
        // 4 unresolved complaints (+20 points)
        // 3 long-pending complaints (+30 points)
        for (int i = 0; i < 4; i++) {
            Complaint c = new Complaint();
            c.setResidentName(testUser.getName());
            c.setResidentUsername(testUser.getEmail());
            c.setDescription("Bad " + i);
            c.setStatus(Complaint.ComplaintStatus.OPEN);
            c.setCategory(Complaint.ComplaintCategory.OTHER);
            c.setContactEmail(testUser.getEmail());
            c.setUser(testUser);
            c = complaintRepository.save(c);

            // Backdate to 4 days ago via JPQL to bypass @PrePersist callback
            entityManager.createQuery("UPDATE Complaint comp SET comp.createdAt = :date WHERE comp.id = :id")
                .setParameter("date", LocalDateTime.now().minusDays(4))
                .setParameter("id", c.getId())
                .executeUpdate();
        }

        entityManager.clear(); // Clear entity manager to force reload

        double cdi = predictionService.calculateCustomerDissatisfactionIndex(testUser);
        // Sentiment: 40, Delay: 30, Unresolved: 20 -> CDI = 90
        assertTrue(cdi >= 75.0, "CDI was: " + cdi);

        // Run weekly analysis
        predictionService.runWeeklyDissatisfactionAnalysis();

        // Check user score is updated in database
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(cdi, updatedUser.getDissatisfactionScore());

        // Check high risk user API list
        List<User> highRisk = predictionService.getHighRiskUsers();
        assertEquals(1, highRisk.size());
        assertEquals(testUser.getEmail(), highRisk.get(0).getEmail());
    }

    @Test
    public void testControllerEndpointsAsAdmin() throws Exception {
        String adminToken = getAuthHeader("admin@example.com", com.vcsm.security.model.UserRole.ROLE_ADMIN);

        // Run analysis via POST
        mockMvc.perform(post("/api/predict/dissatisfaction/run")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Fetch high risk list via GET
        mockMvc.perform(get("/api/predict/dissatisfaction/high-risk")
                .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testControllerEndpointsAsNonAdmin() throws Exception {
        String userToken = getAuthHeader("john.churn@example.com", com.vcsm.security.model.UserRole.ROLE_RESIDENT);

        // Run analysis should be forbidden
        mockMvc.perform(post("/api/predict/dissatisfaction/run")
                .header("Authorization", userToken))
                .andExpect(status().isForbidden());

        // Get high risk list should be forbidden
        mockMvc.perform(get("/api/predict/dissatisfaction/high-risk")
                .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }
}