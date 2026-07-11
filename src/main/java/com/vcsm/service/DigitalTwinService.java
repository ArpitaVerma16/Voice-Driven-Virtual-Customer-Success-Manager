package com.vcsm.service;

import com.vcsm.model.DigitalTwin;
import com.vcsm.repository.DigitalTwinRepository;
import com.vcsm.service.SimulationEngine.SimulationResult;
import com.vcsm.service.SimulationEngine.SimulationScenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@lombok.RequiredArgsConstructor
public class DigitalTwinService {

    private final DigitalTwinRepository digitalTwinRepository;

    private final SimulationEngine simulationEngine;

    private final Map<Long, DigitalTwin> activeTwins = new ConcurrentHashMap<>();

    /**
     * Create a new digital twin
     */
    public DigitalTwin createTwin(DigitalTwin twin) {
        twin.setStatus("CREATED");
        twin.setCreatedAt(LocalDateTime.now());
        DigitalTwin saved = digitalTwinRepository.save(twin);

        // Add to active twins
        if ("ACTIVE".equals(twin.getStatus())) {
            activeTwins.put(saved.getId(), saved);
        }

        return saved;
    }

    /**
     * Sync digital twin with production
     */
    public DigitalTwin syncTwin(Long twinId) {
        DigitalTwin twin = digitalTwinRepository.findById(twinId)
            .orElseThrow(() -> new RuntimeException("Digital twin not found"));

        // Sync data
        String snapshot = captureSnapshot();
        twin.setDataSnapshot(snapshot);
        twin.setLastSync(LocalDateTime.now());
        twin.setStatus("SYNCING");

        return digitalTwinRepository.save(twin);
    }

    private String captureSnapshot() {
        // Simulated snapshot
        return "Snapshot at " + LocalDateTime.now();
    }

    /**
     * Run simulation on twin
     */
    public SimulationResult runSimulation(Long twinId, String scenarioType, int multiplier) {
        DigitalTwin twin = digitalTwinRepository.findById(twinId)
            .orElseThrow(() -> new RuntimeException("Digital twin not found"));

        // Ensure twin is synced
        if (twin.getLastSync() == null || twin.getLastSync().isBefore(LocalDateTime.now().minusMinutes(5))) {
            syncTwin(twinId);
        }

        // Create scenario
        SimulationScenario scenario = new SimulationScenario(
            scenarioType + "_" + System.currentTimeMillis(),
            scenarioType,
            multiplier
        );

        // Run simulation
        SimulationResult result = simulationEngine.runSimulation(twin, scenario);

        // Update twin status
        twin.setStatus("ACTIVE");
        digitalTwinRepository.save(twin);

        return result;
    }

    /**
     * Auto-sync active twins
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    public void autoSyncTwins() {
        List<DigitalTwin> twins = digitalTwinRepository.findByStatus("ACTIVE");
        for (DigitalTwin twin : twins) {
            syncTwin(twin.getId());
        }
    }

    /**
     * Get twin by ID
     */
    public DigitalTwin getTwin(Long twinId) {
        return digitalTwinRepository.findById(twinId).orElse(null);
    }

    /**
     * Get all twins
     */
    public List<DigitalTwin> getAllTwins() {
        return digitalTwinRepository.findAll();
    }

    /**
     * Delete twin
     */
    public void deleteTwin(Long twinId) {
        activeTwins.remove(twinId);
        digitalTwinRepository.deleteById(twinId);
    }

    /**
     * Get twin stats
     */
    public Map<String, Object> getTwinStats() {
        Map<String, Object> stats = new HashMap<>();
        List<DigitalTwin> twins = digitalTwinRepository.findAll();

        stats.put("totalTwins", twins.size());
        stats.put("activeTwins", twins.stream().parallel().filter(t -> "ACTIVE".equals(t.getStatus())).count());
        stats.put("syncedTwins", twins.stream().parallel().filter(t -> t.getLastSync() != null).count());
        stats.put("status", "Digital Twin System active");

        return stats;
    }
}
