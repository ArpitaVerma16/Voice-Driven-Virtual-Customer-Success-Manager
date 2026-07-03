package com.vcsm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.vcsm.model.CustomerSession;
import com.vcsm.repository.CustomerSessionRepository;
import com.vcsm.dto.CustomerJourneyDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerJourneyService {

    @Autowired
    private CustomerSessionRepository customerSessionRepository;

    private static final int DEFAULT_JOURNEY_LIMIT = 20;
    private static final int UNRESOLVED_HIGHLIGHT_LIMIT = 5;

    public List<CustomerJourneyDTO> getCustomerJourney(String customerId) {
        return getCustomerJourney(customerId, DEFAULT_JOURNEY_LIMIT);
    }

    public List<CustomerJourneyDTO> getCustomerJourney(String customerId, int limit) {
        List<CustomerSession> sessions = customerSessionRepository
            .findByCustomerId(customerId, PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "startedAt")))
            .getContent();

        return sessions.stream()
            .map(this::convertToJourneyDTO)
            .sorted(Comparator.comparing(CustomerJourneyDTO::getDate))
            .collect(Collectors.toList());
    }

    public List<CustomerJourneyDTO> getRecentJourney(String customerId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<CustomerSession> sessions = customerSessionRepository
            .findByCustomerIdAndStartedAtAfter(customerId, startDate);

        return sessions.stream()
            .map(this::convertToJourneyDTO)
            .sorted(Comparator.comparing(CustomerJourneyDTO::getDate).reversed())
            .collect(Collectors.toList());
    }

    public Map<String, Object> getCustomerJourneyWithInsights(String customerId) {
        List<CustomerJourneyDTO> journey = getCustomerJourney(customerId, UNRESOLVED_HIGHLIGHT_LIMIT);

        List<String> unresolvedIntents = journey.stream()
            .filter(j -> "unresolved".equals(j.getResolution()))
            .map(CustomerJourneyDTO::getIntent)
            .distinct()
            .collect(Collectors.toList());

        List<CustomerJourneyDTO> recurringIssues = journey.stream()
            .filter(j -> unresolvedIntents.contains(j.getIntent()))
            .collect(Collectors.toList());

        return Map.of(
            "journey", journey,
            "recurring_unresolved_intents", unresolvedIntents,
            "recurring_sessions", recurringIssues,
            "total_sessions", journey.size(),
            "unresolved_count", (int) journey.stream()
                .filter(j -> "unresolved".equals(j.getResolution()))
                .count()
        );
    }

    public List<String> getRecurringUnresolvedIntents(String customerId) {
        return getCustomerJourneyWithInsights(customerId)
            .get("recurring_unresolved_intents").toString()
            .replaceAll("[\\[\\]]", "").split(", ") != null
                ? (List<String>) getCustomerJourneyWithInsights(customerId)
                    .get("recurring_unresolved_intents")
                : new ArrayList<>();
    }

    private CustomerJourneyDTO convertToJourneyDTO(CustomerSession session) {
        CustomerJourneyDTO dto = new CustomerJourneyDTO();
        dto.setSessionId(session.getId());
        dto.setDate(session.getStartedAt().toString());
        dto.setIntent(session.getIntent() != null ? session.getIntent() : "unknown");
        dto.setResolution(session.getResolutionStatus() != null
            ? session.getResolutionStatus()
            : "unresolved");
        dto.setDuration(calculateSessionDuration(session));
        return dto;
    }

    private long calculateSessionDuration(CustomerSession session) {
        if (session.getEndedAt() == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.MINUTES.between(
            session.getStartedAt(),
            session.getEndedAt()
        );
    }
}
