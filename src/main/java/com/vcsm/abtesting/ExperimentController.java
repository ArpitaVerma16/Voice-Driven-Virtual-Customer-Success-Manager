package com.vcsm.abtesting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * REST API for conversation script A/B experiments.
 *
 * Variant lookup is available to any authenticated caller (the conversation
 * runtime needs it per session); outcome comparison reports are admin-only,
 * consistent with the other analytics endpoints.
 */
@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    @Autowired
    private ExperimentService experimentService;

    /**
     * Sticky variant assignment for a session.
     * GET /api/experiments/opening_greeting/variant?sessionId=abc&variants=control,variant_A
     */
    @GetMapping("/{experiment}/variant")
    public ResponseEntity<Map<String, String>> getVariant(
            @PathVariable String experiment,
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "control,variant_A") String variants) {
        List<String> variantList = Arrays.stream(variants.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toList();
        String assigned = experimentService.getVariant(sessionId, experiment, variantList);
        return ResponseEntity.ok(Map.of(
                "experiment", experiment,
                "sessionId", sessionId,
                "variant", assigned));
    }

    /**
     * Record a finished session's outcome for its assigned variant.
     * POST /api/experiments/opening_greeting/outcome
     * {"variant": "control", "resolved": true, "escalated": false, "durationMs": 84000}
     */
    @PostMapping("/{experiment}/outcome")
    public ResponseEntity<Map<String, String>> recordOutcome(
            @PathVariable String experiment,
            @RequestBody OutcomeRequest outcome) {
        if (outcome.variant() == null || outcome.variant().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "variant is required"));
        }
        experimentService.recordOutcome(
                experiment,
                outcome.variant(),
                Boolean.TRUE.equals(outcome.resolved()),
                Boolean.TRUE.equals(outcome.escalated()),
                outcome.durationMs() == null ? 0L : outcome.durationMs());
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }

    /** Per-variant comparison statistics. Admin-only like other analytics. */
    @GetMapping("/{experiment}/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Map<String, Object>>> getReport(@PathVariable String experiment) {
        return ResponseEntity.ok(experimentService.getReport(experiment));
    }

    /** Names of experiments with recorded outcomes. Admin-only. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> listExperiments() {
        return ResponseEntity.ok(experimentService.listExperiments());
    }

    public record OutcomeRequest(String variant, Boolean resolved, Boolean escalated, Long durationMs) {}
}
