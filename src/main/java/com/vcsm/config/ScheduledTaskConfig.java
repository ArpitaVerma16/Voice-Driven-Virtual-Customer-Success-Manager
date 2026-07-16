package com.vcsm.config;

import com.vcsm.service.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTaskConfig {

    private final DeadLetterQueueService dlqService;

    /**
     * Process DLQ retries every minute
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void processDLQRetries() {
        try {
            dlqService.processRetries();
        } catch (Exception e) {
            log.error("Error processing DLQ retries", e);
        }
    }

    /**
     * Cleanup old entries daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupDLQ() {
        try {
            int deleted = dlqService.cleanupOldEntries(30);
            if (deleted > 0) {
                log.info("Daily DLQ cleanup: deleted {} entries", deleted);
            }
        } catch (Exception e) {
            log.error("Error during DLQ cleanup", e);
        }
    }

    /**
     * Send alert if failed entries exceed threshold (every hour)
     */
    @Scheduled(fixedDelay = 3600000)
    public void checkDLQHealth() {
        try {
            var stats = dlqService.getStatistics();
            if (stats.getFailed() > 10) {
                log.warn("⚠️ DLQ health alert: {} failed entries waiting", stats.getFailed());
                // Could integrate with notification service
            }
        } catch (Exception e) {
            log.error("Error checking DLQ health", e);
        }
    }
}