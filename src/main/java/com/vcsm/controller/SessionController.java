package com.vcsm.controller;

import com.vcsm.model.CustomerSession;
import com.vcsm.model.SessionTurn;
import com.vcsm.service.SessionManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/sessions")
@lombok.RequiredArgsConstructor
public class SessionController {

    private final SessionManagementService sessionManagementService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(@Valid @RequestBody Map<String, String> request) {
        String customerId = request.get("customerId");

        if (customerId == null || customerId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "customerId required"));
        }

        CustomerSession session = sessionManagementService.createSession(customerId);
        return ResponseEntity.ok(Map.of(
            "sessionId", session.getId(),
            "customerId", session.getCustomerId(),
            "startedAt", session.getStartedAt(),
            "success", true
        ));
    }

    @PostMapping("/{sessionId}/turn")
    public ResponseEntity<Map<String, String>> addTurn(
            @PathVariable String sessionId,
            @Valid @RequestBody Map<String, String> request) {
        String speaker = request.get("speaker");
        String content = request.get("content");

        if (speaker == null || content == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "speaker and content required"));
        }

        sessionManagementService.addSessionTurn(sessionId, speaker, content);
        return ResponseEntity.ok(Map.of("success", "true"));
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endSession(
            @PathVariable String sessionId,
            @Valid @RequestBody Map<String, String> request) {
        String intent = request.get("intent");
        String resolutionStatus = request.getOrDefault("resolutionStatus", "unresolved");

        sessionManagementService.endSession(sessionId, intent, resolutionStatus);
        return ResponseEntity.ok(Map.of("success", true, "sessionId", sessionId));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<Map<String, Object>> getSessions(@PathVariable String customerId) {
        List<CustomerSession> sessions = sessionManagementService.getRecentSessions(customerId);

        List<Map<String, Object>> sessionList = new ArrayList<>();
        for (CustomerSession session : sessions) {
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("id", session.getId());
            sessionData.put("startedAt", session.getStartedAt());
            sessionData.put("endedAt", session.getEndedAt());
            sessionData.put("durationSeconds", session.getDurationSeconds());
            sessionData.put("intent", session.getIntent());
            sessionData.put("resolutionStatus", session.getResolutionStatus());
            sessionData.put("turnCount", session.getTurns() != null ? session.getTurns().size() : 0);
            sessionList.add(sessionData);
        }

        return ResponseEntity.ok(Map.of(
            "customerId", customerId,
            "sessions", sessionList,
            "totalSessions", sessionManagementService.getSessionCountForCustomer(customerId)
        ));
    }

    @GetMapping("/{sessionId}/transcript")
    public ResponseEntity<Map<String, Object>> getSessionTranscript(@PathVariable String sessionId) {
        Optional<CustomerSession> sessionOpt = sessionManagementService.getSessionWithTranscript(sessionId);

        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CustomerSession session = sessionOpt.get();
        List<SessionTurn> turns = session.getTurns();

        List<Map<String, Object>> turnList = new ArrayList<>();
        for (SessionTurn turn : turns) {
            Map<String, Object> turnData = new HashMap<>();
            turnData.put("index", turn.getTurnIndex());
            turnData.put("speaker", turn.getSpeaker());
            turnData.put("content", turn.getContent());
            turnData.put("timestamp", turn.getTimestamp());
            turnList.add(turnData);
        }

        return ResponseEntity.ok(Map.of(
            "sessionId", session.getId(),
            "customerId", session.getCustomerId(),
            "intent", session.getIntent(),
            "resolutionStatus", session.getResolutionStatus(),
            "startedAt", session.getStartedAt(),
            "endedAt", session.getEndedAt(),
            "transcript", turnList
        ));
    }

    @PatchMapping("/{sessionId}/resolution")
    public ResponseEntity<Map<String, String>> updateResolution(
            @PathVariable String sessionId,
            @Valid @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");

        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status required"));
        }

        sessionManagementService.updateResolutionStatus(sessionId, newStatus);
        return ResponseEntity.ok(Map.of("success", "true"));
    }
}

