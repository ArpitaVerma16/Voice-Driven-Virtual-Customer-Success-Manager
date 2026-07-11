package com.vcsm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vcsm.dto.CustomerJourneyDTO;
import com.vcsm.model.CustomerSession;
import com.vcsm.repository.CustomerSessionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CustomerJourneyServiceTest {

    @Autowired
    private CustomerJourneyService journeyService;

    @Autowired
    private CustomerSessionRepository sessionRepository;

    private String testCustomerId = "CUST_TEST_001";

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
    }

    @Test
    void testGetCustomerJourneyEmpty() {
        List<CustomerJourneyDTO> journey = journeyService.getCustomerJourney(testCustomerId);
        assertTrue(journey.isEmpty());
    }

    @Test
    void testGetCustomerJourneyOrdering() {
        CustomerSession session1 = new CustomerSession(testCustomerId);
        session1.setIntent("billing");
        session1.setStartedAt(LocalDateTime.now().minusDays(2));
        session1.setResolutionStatus("resolved");

        CustomerSession session2 = new CustomerSession(testCustomerId);
        session2.setIntent("technical");
        session2.setStartedAt(LocalDateTime.now().minusDays(1));
        session2.setResolutionStatus("unresolved");

        sessionRepository.save(session1);
        sessionRepository.save(session2);

        List<CustomerJourneyDTO> journey = journeyService.getCustomerJourney(testCustomerId);
        assertEquals(2, journey.size());
        assertEquals("billing", journey.get(0).getIntent());
        assertEquals("technical", journey.get(1).getIntent());
    }

    @Test
    void testGetCustomerJourneyWithInsights() {
        CustomerSession session1 = new CustomerSession(testCustomerId);
        session1.setIntent("billing");
        session1.setResolutionStatus("unresolved");

        CustomerSession session2 = new CustomerSession(testCustomerId);
        session2.setIntent("billing");
        session2.setResolutionStatus("unresolved");

        sessionRepository.save(session1);
        sessionRepository.save(session2);

        Map<String, Object> insights = journeyService.getCustomerJourneyWithInsights(testCustomerId);

        assertEquals(2, insights.get("total_sessions"));
        assertEquals(2, insights.get("unresolved_count"));
    }

    @Test
    void testGetRecentJourney() {
        CustomerSession oldSession = new CustomerSession(testCustomerId);
        oldSession.setIntent("billing");
        oldSession.setStartedAt(LocalDateTime.now().minusDays(10));

        CustomerSession recentSession = new CustomerSession(testCustomerId);
        recentSession.setIntent("technical");
        recentSession.setStartedAt(LocalDateTime.now().minusDays(2));

        sessionRepository.save(oldSession);
        sessionRepository.save(recentSession);

        List<CustomerJourneyDTO> recent = journeyService.getRecentJourney(testCustomerId, 5);
        assertEquals(1, recent.size());
        assertEquals("technical", recent.get(0).getIntent());
    }
}
