package com.vcsm.abtesting;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Lightweight A/B testing for conversation script variations (issue #473).
 *
 * Variant assignment hashes sessionId:experiment, so a session always lands
 * in the same cohort (sticky assignment) without storing any mapping, and
 * different experiments split the same session pool independently.
 *
 * Outcome tracking accumulates per-variant counters (sessions, resolutions,
 * escalations, total duration) in memory using concurrent adders; the
 * report endpoint derives resolution rate, escalation rate, and average
 * session duration per variant. Counters reset on application restart;
 * persisting them is intentionally out of scope for this first iteration.
 */
@Service
public class ExperimentService {

    /** Per-variant outcome counters, concurrent and lock-free on the hot path. */
    static final class VariantStats {
        final LongAdder sessions = new LongAdder();
        final LongAdder resolved = new LongAdder();
        final LongAdder escalated = new LongAdder();
        final LongAdder totalDurationMs = new LongAdder();
    }

    private final Map<String, Map<String, VariantStats>> experiments = new ConcurrentHashMap<>();

    /**
     * Deterministically assign a session to a variant cohort.
     *
     * @param sessionId  stable id of the conversation session
     * @param experiment experiment name, e.g. "opening_greeting"
     * @param variants   cohort labels, e.g. ["control", "variant_A"]
     * @return the assigned variant label, identical for every call with the
     *         same sessionId and experiment
     */
    public String getVariant(String sessionId, String experiment, List<String> variants) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (experiment == null || experiment.isBlank()) {
            throw new IllegalArgumentException("experiment must not be blank");
        }
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("variants must not be empty");
        }
        byte[] digest = md5(sessionId + ":" + experiment);
        // First 8 bytes as an unsigned value is plenty for a uniform bucket split
        long hash = 0;
        for (int i = 0; i < 8; i++) {
            hash = (hash << 8) | (digest[i] & 0xffL);
        }
        int index = (int) Long.remainderUnsigned(hash, variants.size());
        return variants.get(index);
    }

    /**
     * Record the outcome of one finished session for a variant.
     *
     * @param experiment experiment name
     * @param variant    variant label the session was assigned to
     * @param resolved   whether the session ended resolved
     * @param escalated  whether the session was escalated to a human
     * @param durationMs session duration in milliseconds
     */
    public void recordOutcome(String experiment, String variant,
                              boolean resolved, boolean escalated, long durationMs) {
        VariantStats stats = experiments
                .computeIfAbsent(experiment, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(variant, k -> new VariantStats());
        stats.sessions.increment();
        if (resolved) stats.resolved.increment();
        if (escalated) stats.escalated.increment();
        stats.totalDurationMs.add(Math.max(0, durationMs));
    }

    /**
     * Per-variant comparison statistics for an experiment.
     *
     * @return map of variant label to {sessions, resolutionRate,
     *         escalationRate, avgDurationMs}; empty when the experiment has
     *         no recorded outcomes yet
     */
    public Map<String, Map<String, Object>> getReport(String experiment) {
        Map<String, Map<String, Object>> report = new LinkedHashMap<>();
        Map<String, VariantStats> variants = experiments.get(experiment);
        if (variants == null) {
            return report;
        }
        for (Map.Entry<String, VariantStats> e : variants.entrySet()) {
            VariantStats s = e.getValue();
            long n = s.sessions.sum();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sessions", n);
            row.put("resolutionRate", n > 0 ? round(s.resolved.sum() * 100.0 / n) : 0.0);
            row.put("escalationRate", n > 0 ? round(s.escalated.sum() * 100.0 / n) : 0.0);
            row.put("avgDurationMs", n > 0 ? s.totalDurationMs.sum() / n : 0L);
            report.put(e.getKey(), row);
        }
        return report;
    }

    /** Names of all experiments with at least one recorded outcome. */
    public List<String> listExperiments() {
        return List.copyOf(experiments.keySet());
    }

    private static byte[] md5(String input) {
        try {
            return MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // MD5 is mandated by the JCA spec; unreachable on a compliant JVM
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
