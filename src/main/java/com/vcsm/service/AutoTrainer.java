package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.ModelVersion;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.ModelVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Profile("dev")
@Service
@lombok.RequiredArgsConstructor
public class AutoTrainer {

    private final ComplaintRepository complaintRepository;

    private final ModelVersionRepository modelVersionRepository;

    private final DriftDetector driftDetector;

    /**
     * Train new model version
     */
    public ModelVersion trainNewModel(String modelName) {
        // Check for drift
        DriftDetector.DriftResult drift = driftDetector.detectDrift();

        List<Complaint> trainingData = complaintRepository.findAll();
        if (trainingData.isEmpty()) {
            throw new CustomDomainException("No training data available");
        }

        // Simulate training
        long startTime = System.currentTimeMillis();
        ModelVersion newVersion = createModelVersion(modelName, trainingData.size());
        long trainingTime = System.currentTimeMillis() - startTime;
        newVersion.setTrainingTime(trainingTime);
        newVersion.setPerformanceMetrics(generateMetrics());

        // If drift detected, flag for review
        if (drift.isHasDrift()) {
            newVersion.setActive(false);
            newVersion.setDeployed(false);
        } else {
            newVersion.setActive(true);
            newVersion.setDeployed(true);
            newVersion.setDeployedAt(LocalDateTime.now());
        }

        // Deactivate previous versions
        if (newVersion.isActive()) {
            deactivatePreviousVersions(modelName);
        }

        return modelVersionRepository.save(newVersion);
    }

    private ModelVersion createModelVersion(String modelName, int dataSize) {
        ModelVersion version = new ModelVersion();
        version.setModelName(modelName);
        version.setVersion(generateVersion());
        version.setAccuracy(0.75 + new Random().nextDouble() * 0.2);
        version.setPrecisionScore(0.7 + new Random().nextDouble() * 0.25);
        version.setRecallScore(0.7 + new Random().nextDouble() * 0.25);
        version.setF1Score(0.7 + new Random().nextDouble() * 0.25);
        version.setTrainingDataSize(dataSize);
        version.setDeployed(false);
        version.setActive(false);
        return version;
    }

    private String generateVersion() {
        return "v" + (System.currentTimeMillis() / 1000);
    }

    private String generateMetrics() {
        return String.format("{\"accuracy\": %.2f, \"f1\": %.2f, \"precision\": %.2f, \"recall\": %.2f}",
            0.8 + new Random().nextDouble() * 0.15,
            0.75 + new Random().nextDouble() * 0.2,
            0.75 + new Random().nextDouble() * 0.2,
            0.75 + new Random().nextDouble() * 0.2);
    }

    private void deactivatePreviousVersions(String modelName) {
        List<ModelVersion> versions = modelVersionRepository.findByModelNameOrderByCreatedAtDesc(modelName);
        for (ModelVersion v : versions) {
            v.setActive(false);
            v.setDeployed(false);
            modelVersionRepository.save(v);
        }
    }
}
