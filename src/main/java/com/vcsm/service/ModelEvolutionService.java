package com.vcsm.service;

import com.vcsm.model.ModelVersion;
import com.vcsm.repository.ModelVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@lombok.RequiredArgsConstructor
public class ModelEvolutionService {

    private final ModelVersionRepository modelVersionRepository;

    private final AutoTrainer autoTrainer;

    private final DriftDetector driftDetector;

    /**
     * Run evolution cycle automatically
     */
    @Scheduled(cron = "0 0 2 * * MON") // Every Monday at 2 AM
    public void runEvolutionCycle() {
        System.out.println("🧬 Starting model evolution cycle...");

        // Check drift
        DriftDetector.DriftResult drift = driftDetector.detectDrift();

        if (drift.isHasDrift()) {
            System.out.println("⚠️ Data drift detected! Retraining models...");
            retrainAllModels();
        } else {
            System.out.println("✅ No significant drift. Models are healthy.");
        }

        System.out.println("✅ Evolution cycle completed");
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
                System.out.println("✅ Model '" + modelName + "' retrained. New version: " + newVersion.getVersion());
            } catch (Exception e) {
                System.err.println("❌ Failed to retrain model '" + modelName + "': " + e.getMessage());
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
        long deployedCount = allVersions.stream().filter(ModelVersion::isDeployed).count();

        stats.put("totalVersions", allVersions.size());
        stats.put("deployedModels", deployedCount);
        stats.put("models", getModelNames());
        stats.put("status", "Evolution system active");

        return stats;
    }
}