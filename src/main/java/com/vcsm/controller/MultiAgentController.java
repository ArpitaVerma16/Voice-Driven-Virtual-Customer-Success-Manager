package com.vcsm.controller;

import com.vcsm.model.AgentRequest;
import com.vcsm.service.MultiAgentOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class MultiAgentController {

    @Autowired
    private MultiAgentOrchestrator orchestrator;

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> process(@RequestBody AgentRequest request) {
        Map<String, Object> result = orchestrator.processRequest(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/intents")
    public ResponseEntity<Map<String, Object>> getIntents(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("intents", new AgentRouter().detectIntents(query));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "Multi-Agent System is running"));
    }
}