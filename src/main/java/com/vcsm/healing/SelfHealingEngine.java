package com.vcsm.healing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@lombok.RequiredArgsConstructor
public class SelfHealingEngine {

    private static final Logger log = LoggerFactory.getLogger(SelfHealingEngine.class);

    private final AnomalyDetector anomalyDetector;

    private final AutoRecoveryService autoRecoveryService;

    private final List<HealingReport> healingReports = new ArrayList<>();
    private int healingCount = 0;

    /**
     * Run self-healing cycle every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void runHealingCycle() {
        log.info("🔍 Running self-healing cycle...");

        // Collect system metrics
        Map<String, Double> metrics = collectMetrics();

        // Detect anomalies
        AnomalyDetector.AnomalyResult result = anomalyDetector.detectAnomalies(metrics);

        if (!result.isHasAnomalies()) {
            log.info("✅ No anomalies detected. System is healthy.");
            return;
        }

        log.info("⚠️ " + result.getAnomalies().size() + " anomalies detected!");

        // Handle each anomaly
        for (AnomalyDetector.Anomaly anomaly : result.getAnomalies()) {
            log.info("🔄 Executing recovery for: " + anomaly.getMetricName());
            AutoRecoveryService.RecoveryResult recovery = autoRecoveryService.executeRecovery(anomaly);

            healingCount++;

            HealingReport report = new HealingReport(
                healingCount,
                anomaly,
                recovery,
                new Date()
            );
            healingReports.add(report);

            log.info("✅ Recovery " + recovery.getStatus() + ": " + recovery.getMessage());
        }
    }

    private Map<String, Double> collectMetrics() {
        Map<String, Double> metrics = new HashMap<>();

        // Simulated metrics collection
        // In production, collect from actual system monitoring
        Random random = new Random();

        metrics.put("error_rate", random.nextDouble() * 10);
        metrics.put("response_time", 100 + random.nextDouble() * 400);
        metrics.put("memory_usage", 40 + random.nextDouble() * 60);
        metrics.put("cpu_usage", 20 + random.nextDouble() * 80);
        metrics.put("thread_pool_usage", 30 + random.nextDouble() * 70);
        metrics.put("failure_rate", random.nextDouble() * 20);

        return metrics;
    }

    /**
     * Get healing report
     */
    public HealingReport getHealingReport(int index) {
        if (index < 0 || index >= healingReports.size()) {
            return null;
        }
        return healingReports.get(index);
    }

    /**
     * Get all healing reports
     */
    public List<HealingReport> getAllHealingReports() {
        return new ArrayList<>(healingReports);
    }

    /**
     * Get healing statistics
     */
    public Map<String, Object> getHealingStats() {
        Map<String, Object> stats = new HashMap<>();

        long successCount = healingReports.stream()
            .filter(r -> "SUCCESS".equals(r.getRecovery().getStatus()))
            .count();

        stats.put("totalHealings", healingReports.size());
        stats.put("successRate", healingReports.isEmpty() ? 0 : (successCount * 100.0 / healingReports.size()));
        stats.put("lastHealing", healingReports.isEmpty() ? null : healingReports.get(healingReports.size() - 1).getTimestamp());

        return stats;
    }

    public static class HealingReport {
        private final int id;
        private final AnomalyDetector.Anomaly anomaly;
        private final AutoRecoveryService.RecoveryResult recovery;
        private final Date timestamp;

        public HealingReport(int id, AnomalyDetector.Anomaly anomaly, AutoRecoveryService.RecoveryResult recovery, Date timestamp) {
            this.id = id;
            this.anomaly = anomaly;
            this.recovery = recovery;
            this.timestamp = timestamp;
        }

        public int getId() { return id; }
        public AnomalyDetector.Anomaly getAnomaly() { return anomaly; }
        public AutoRecoveryService.RecoveryResult getRecovery() { return recovery; }
        public Date getTimestamp() { return timestamp; }
    }
}