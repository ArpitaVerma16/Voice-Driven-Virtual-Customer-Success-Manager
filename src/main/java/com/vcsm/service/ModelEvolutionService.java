package com.vcsm.service;

import com.vcsm.model.ModelVersion;
import com.vcsm.repository.ModelVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@lombok.RequiredArgsConstructor
public class ModelEvolutionService {
    private static final Logger log = LoggerFactory.getLogger(ModelEvolutionService.class);

    private static final Logger log = LoggerFactory.getLogger(ModelEvolutionService.class);

    private final ModelVersionRepository modelVersionRepository;

    private final AutoTrainer autoTrainer;

    private final DriftDetector driftDetector;

    /**
     * Run evolution cycle automatically
     */
    @Scheduled(cron = "0 0 2 * * MON") // Every Monday at 2 AM
    public void runEvolutionCycle() {
        log.info("🧬 Starting model evolution cycle...");

        // Check drift
        DriftDetector.DriftResult drift = driftDetector.detectDrift();

        if (drift.isHasDrift()) {
            log.info("⚠️ Data drift detected! Retraining models...");
            retrainAllModels();
        } else {
            log.info("✅ No significant drift. Models are healthy.");
        }

        log.info("✅ Evolution cycle completed");
    }

    /**
     * Train new model
     */
    public ModelVersion trainModel(String modelName) {
        try {
            return autoTrainer.trainNewModel(modelName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to train model: " + e.getMessage(), e);
        }
    }

    private void retrainAllModels() {
        List<String> modelNames = getModelNames();
        for (String modelName : modelNames) {
            try {
                ModelVersion newVersion = autoTrainer.trainNewModel(modelName);
                log.info("✅ Model '" + modelName + "' retrained. New version: " + newVersion.getVersion());
                log.info("✅ Model '{}' retrained. New version: {}", modelName, newVersion.getVersion());
            } catch (Exception e) {
                log.error("❌ Failed to retrain model '{}': {}", modelName, e.getMessage());
            }
        }
    }

    private List<String> getModelNames() {
        return Arrays.asList("complaint_classifier", "sentiment_analyzer", "priority_predictor");
    }

    /**
     * Get model evolution history
     */
    public List<ModelVersion> getEvolutionHistory(String modelName) {
        return modelVersionRepository.findByModelNameOrderByCreatedAtDesc(modelName);
    }

    /**
     * Get current active model
     */
    public ModelVersion getActiveModel(String modelName) {
        return modelVersionRepository.findByModelNameAndIsActiveTrue(modelName).orElse(null);
    }

    /**
     * Rollback to previous version
     */
    public ModelVersion rollback(String modelName) {
        List<ModelVersion> versions = modelVersionRepository.findByModelNameOrderByCreatedAtDesc(modelName);
        if (versions.size() < 2) {
            throw new RuntimeException("No previous version to rollback to");
        }

        // Deactivate current
        versions.get(0).setActive(false);
        versions.get(0).setDeployed(false);
        modelVersionRepository.save(versions.get(0));

        // Activate previous
        versions.get(1).setActive(true);
        versions.get(1).setDeployed(true);
        versions.get(1).setDeployedAt(new Date().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        modelVersionRepository.save(versions.get(1));

        return versions.get(1);
    }

    /**
     * Get evolution statistics
     */
    public Map<String, Object> getEvolutionStats() {
        Map<String, Object> stats = new HashMap<>();
        List<ModelVersion> allVersions = modelVersionRepository.findAll();
        long deployedCount = allVersions.stream().parallel().filter(ModelVersion::isDeployed).count();

        stats.put("totalVersions", allVersions.size());
        stats.put("deployedModels", deployedCount);
        stats.put("models", getModelNames());
        stats.put("status", "Evolution system active");

        return stats;
    }
}