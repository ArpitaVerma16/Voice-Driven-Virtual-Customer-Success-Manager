package com.vcsm.abtesting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentServiceTest {

    private ExperimentService service;

    private static final List<String> VARIANTS = List.of("control", "variant_A");

    @BeforeEach
    void setUp() {
        service = new ExperimentService();
    }

    @Test
    void assignmentIsSticky() {
        String first = service.getVariant("session-123", "opening_greeting", VARIANTS);
        for (int i = 0; i < 50; i++) {
            assertEquals(first, service.getVariant("session-123", "opening_greeting", VARIANTS));
        }
    }

    @Test
    void differentExperimentsSplitIndependently() {
        // The same session may land in different cohorts for different
        // experiments; verify assignments differ for at least one session,
        // proving the experiment name participates in the hash.
        boolean anyDifferent = false;
        for (int i = 0; i < 200 && !anyDifferent; i++) {
            String a = service.getVariant("s" + i, "exp_one", VARIANTS);
            String b = service.getVariant("s" + i, "exp_two", VARIANTS);
            anyDifferent = !a.equals(b);
        }
        assertTrue(anyDifferent);
    }

    @Test
    void assignmentIsRoughlyUniform() {
        Map<String, Integer> counts = new HashMap<>();
        int n = 2000;
        for (int i = 0; i < n; i++) {
            String v = service.getVariant("session-" + i, "opening_greeting", VARIANTS);
            counts.merge(v, 1, Integer::sum);
        }
        // With 2000 sessions over 2 cohorts, each side should hold 40-60%
        for (String variant : VARIANTS) {
            int c = counts.getOrDefault(variant, 0);
            assertTrue(c > n * 0.4 && c < n * 0.6,
                    variant + " got " + c + " of " + n + " sessions");
        }
    }

    @Test
    void assignmentCoversAllVariantsOfThreeWaySplit() {
        List<String> three = List.of("control", "variant_A", "variant_B");
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 300; i++) {
            counts.merge(service.getVariant("s" + i, "greeting", three), 1, Integer::sum);
        }
        assertEquals(3, counts.size());
    }

    @Test
    void invalidArgumentsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getVariant("", "exp", VARIANTS));
        assertThrows(IllegalArgumentException.class,
                () -> service.getVariant("s", "", VARIANTS));
        assertThrows(IllegalArgumentException.class,
                () -> service.getVariant("s", "exp", List.of()));
    }

    @Test
    void reportComputesRatesPerVariant() {
        service.recordOutcome("greeting", "control", true, false, 60_000);
        service.recordOutcome("greeting", "control", false, true, 120_000);
        service.recordOutcome("greeting", "variant_A", true, false, 30_000);

        Map<String, Map<String, Object>> report = service.getReport("greeting");

        Map<String, Object> control = report.get("control");
        assertEquals(2L, control.get("sessions"));
        assertEquals(50.0, control.get("resolutionRate"));
        assertEquals(50.0, control.get("escalationRate"));
        assertEquals(90_000L, control.get("avgDurationMs"));

        Map<String, Object> variantA = report.get("variant_A");
        assertEquals(1L, variantA.get("sessions"));
        assertEquals(100.0, variantA.get("resolutionRate"));
        assertEquals(0.0, variantA.get("escalationRate"));
        assertEquals(30_000L, variantA.get("avgDurationMs"));
    }

    @Test
    void reportForUnknownExperimentIsEmpty() {
        assertTrue(service.getReport("never_ran").isEmpty());
    }

    @Test
    void negativeDurationDoesNotCorruptAverages() {
        service.recordOutcome("greeting", "control", true, false, -500);
        assertEquals(0L, service.getReport("greeting").get("control").get("avgDurationMs"));
    }

    @Test
    void listExperimentsReturnsRecordedOnes() {
        service.recordOutcome("exp_one", "control", true, false, 1000);
        service.recordOutcome("exp_two", "control", false, false, 1000);
        List<String> names = service.listExperiments();
        assertTrue(names.contains("exp_one"));
        assertTrue(names.contains("exp_two"));
    }
}
