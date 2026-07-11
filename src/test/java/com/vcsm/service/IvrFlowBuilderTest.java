package com.vcsm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import com.vcsm.security.hmac.SignatureValidator;
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

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@lombok.RequiredArgsConstructor
public class IvrFlowBuilderTest {

    private final MockMvc mockMvc;

    private final UserRepository userRepository;

    private final AppUserRepository appUserRepository;

    private final JwtService jwtService;

    private final com.vcsm.security.service.UserDetailsServiceImpl userDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper;

    private final SignatureValidator signatureValidator;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    public void setUp() {
        testUser = userRepository.findByEmail("admin.ivr@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin IVR Owner");
            u.setEmail("admin.ivr@example.com");
            u.setPassword("password");
            return userRepository.save(u);
        });

        if (appUserRepository.findByUsername("admin.ivr@example.com").isEmpty()) {
            AppUser appUser = new AppUser();
            appUser.setUsername("admin.ivr@example.com");
            appUser.setPasswordHash(passwordEncoder.encode("password"));
            appUser.addRole(UserRole.ROLE_ADMIN);
            appUserRepository.save(appUser);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin.ivr@example.com");
        jwtToken = jwtService.generateToken(userDetails);
    }

    /**
     * Compute HMAC headers for /api/voice/command POST requests.
     * Matches the signature algorithm in SignatureValidator.
     */
    private String[] buildHmacHeaders(String body) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        String signature = signatureValidator.generateSignature("POST", "/api/voice/command", body, timestamp, nonce);
        return new String[]{timestamp, nonce, signature};
    }

    @Test
    public void testGetAndSaveFlowConfig() throws Exception {
        // 1. GET current flow configuration
        mockMvc.perform(get("/api/voice/flow-config")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("root"))
                .andExpect(jsonPath("$.prompt").isNotEmpty());

        // 2. POST to update flow configuration
        String customFlow = "{\n" +
                "  \"id\": \"root\",\n" +
                "  \"prompt\": \"Welcome to custom test menu. Say 'info' for help.\",\n" +
                "  \"options\": [\n" +
                "    {\n" +
                "      \"id\": \"info_node\",\n" +
                "      \"pattern\": \"info|help\",\n" +
                "      \"prompt\": \"This is a custom prompt info text.\",\n" +
                "      \"action\": \"action_info\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        mockMvc.perform(post("/api/voice/flow-config")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("flowJson", customFlow))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Verify custom flow is now active
        mockMvc.perform(get("/api/voice/flow-config")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt").value("Welcome to custom test menu. Say 'info' for help."))
                .andExpect(jsonPath("$.options[0].id").value("info_node"));
    }

    @Test
    public void testDynamicIvrTraversalAndReset() throws Exception {
        // Ensure the default IVR flow is active before traversal (independent of other tests)
        String defaultFlow = "{\"id\":\"root\",\"prompt\":\"Welcome to VCSM voice support. Say '1' for complaints, or '2' for community events.\",\"options\":[{\"id\":\"complaints_menu\",\"pattern\":\"1|one|complaint|complaints\",\"prompt\":\"You selected complaints. Say 'new' to file a new complaint, or 'status' to check existing complaints.\",\"options\":[{\"id\":\"file_complaint\",\"pattern\":\"new|file|create\",\"prompt\":\"Redirecting you to the file complaint screen. Please describe your issue.\",\"action\":\"action_file_complaint\"},{\"id\":\"view_complaints\",\"pattern\":\"status|check|existing\",\"prompt\":\"Opening your active complaints board.\",\"action\":\"action_view_complaints\"}]},{\"id\":\"events_menu\",\"pattern\":\"2|two|event|events\",\"prompt\":\"You selected events. Say 'book' to book a new event ticket, or 'list' to view available events.\",\"options\":[{\"id\":\"book_event\",\"pattern\":\"book|reserve|register\",\"prompt\":\"Redirecting to the event registration section.\",\"action\":\"action_book_event\"},{\"id\":\"view_events\",\"pattern\":\"list|view|show\",\"prompt\":\"Opening the community events board.\",\"action\":\"action_view_events\"}]}]}";

        mockMvc.perform(post("/api/voice/flow-config")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("flowJson", defaultFlow))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 1: Reset session with "hello" – start at root
        String body1 = objectMapper.writeValueAsString(Map.of("transcript", "hello"));
        String[] hmac1 = buildHmacHeaders(body1);

        mockMvc.perform(post("/api/voice/command")
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Timestamp", hmac1[0])
                .header("X-Nonce", hmac1[1])
                .header("X-Signature", hmac1[2])
                .contentType(MediaType.APPLICATION_JSON)
                .content(body1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentNodeId").value("root"));

        // Step 2: Navigate to complaints menu by saying "complaint"
        String body2 = objectMapper.writeValueAsString(Map.of("transcript", "complaint"));
        String[] hmac2 = buildHmacHeaders(body2);

        mockMvc.perform(post("/api/voice/command")
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Timestamp", hmac2[0])
                .header("X-Nonce", hmac2[1])
                .header("X-Signature", hmac2[2])
                .contentType(MediaType.APPLICATION_JSON)
                .content(body2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentNodeId").value("complaints_menu"));

        // Step 3: Select "new" – triggers action_file_complaint and ends session
        String body3 = objectMapper.writeValueAsString(Map.of("transcript", "new"));
        String[] hmac3 = buildHmacHeaders(body3);

        mockMvc.perform(post("/api/voice/command")
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Timestamp", hmac3[0])
                .header("X-Nonce", hmac3[1])
                .header("X-Signature", hmac3[2])
                .contentType(MediaType.APPLICATION_JSON)
                .content(body3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentNodeId").value("file_complaint"))
                .andExpect(jsonPath("$.action").value("action_file_complaint"));
    }
}
