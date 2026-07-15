package com.vcsm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import com.vcsm.security.jwt.JwtService;
import com.vcsm.security.model.AppUser;
import com.vcsm.security.model.UserRole;
import com.vcsm.security.repo.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@lombok.RequiredArgsConstructor
public class VoiceCallSummarizationTest {

    private final MockMvc mockMvc;

    private final GenAIResolver genAIResolver;

    private final UserRepository userRepository;

    private final AppUserRepository appUserRepository;

    private final ComplaintRepository complaintRepository;

    private final JwtService jwtService;

    private final com.vcsm.security.service.UserDetailsServiceImpl userDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    public void setUp() {
        complaintRepository.deleteAll();
        
        // Create test user if not already present
        testUser = userRepository.findByEmail("resident.call@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Resident Call Owner");
            u.setEmail("resident.call@example.com");
            return userRepository.save(u);
        });

        // Create app user if not already present
        if (appUserRepository.findByUsername("resident.call@example.com").isEmpty()) {
            AppUser appUser = new AppUser();
            appUser.setUsername("resident.call@example.com");
            appUser.setPasswordHash(passwordEncoder.encode("password"));
            appUser.addRole(UserRole.ROLE_RESIDENT);
            appUserRepository.save(appUser);
        }

        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername("resident.call@example.com");
        jwtToken = jwtService.generateToken(userDetails);
    }

    @Test
    public void testSummarizeAndAutoTicketWaterLeak() throws Exception {
        String transcript = "Resident: Hello, I have a big problem. There is a water leak in my kitchen.\n" +
                "Agent: I understand. I will file a ticket for you.\n";

        mockMvc.perform(post("/api/genai/summarize")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "transcript", transcript,
                        "residentEmail", "resident.call@example.com"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Resident called regarding a water maintenance issue. They reported a potential leak or equipment malfunction."))
                .andExpect(jsonPath("$.identifiedIssues[0]").value("Utility Maintenance: Water motor/pipe failure"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.ticketGenerated").value(true))
                .andExpect(jsonPath("$.generatedTicketId").isNotEmpty());

        // Assert that complaint is saved in DB
        List<Complaint> complaints = complaintRepository.findByResidentUsernameOrderByCreatedAtDesc("resident.call@example.com");
        assertFalse(complaints.isEmpty());
        Complaint filed = complaints.get(0);
        assertTrue(filed.getDescription().contains("water leak in my kitchen"));
        assertEquals("HIGH", filed.getPriority());
        assertEquals(Complaint.ComplaintCategory.UTILITIES, filed.getCategory());
    }

    @Test
    public void testSummarizeNoiseComplaintDisturbance() throws Exception {
        String transcript = "Resident: The neighbors are playing loud music again. It is very disturbing.\n" +
                "Agent: Sorry about that, I am assigning a security personnel.\n";

        mockMvc.perform(post("/api/genai/summarize")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "transcript", transcript,
                        "residentEmail", "resident.call@example.com"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Resident called to report a noise disturbance. Loud sounds are disrupting the peace of the residential area."))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.ticketGenerated").value(true));

        List<Complaint> complaints = complaintRepository.findByResidentUsernameOrderByCreatedAtDesc("resident.call@example.com");
        assertFalse(complaints.isEmpty());
        Complaint filed = complaints.get(0);
        assertEquals(Complaint.ComplaintCategory.NOISE, filed.getCategory());
        assertEquals("MEDIUM", filed.getPriority());
    }
}
