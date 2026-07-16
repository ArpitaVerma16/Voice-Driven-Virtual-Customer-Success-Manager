package com.vcsm.dlq;

import com.vcsm.model.dlq.DeadLetterQueueEntry;
import com.vcsm.service.DeadLetterQueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class DeadLetterQueueTest {

    @Autowired
    private DeadLetterQueueService dlqService;

    @Test
    void shouldAddToDLQ() {
        RuntimeException testError = new RuntimeException("Test error");
        
        DeadLetterQueueEntry entry = dlqService.addToDeadLetterQueue(
            "TEST_QUEUE",
            Map.of("key", "value"),
            testError
        );
        
        assertThat(entry).isNotNull();
        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getStatus()).isEqualTo(DeadLetterQueueEntry.DLQStatus.PENDING);
        assertThat(entry.getFailureReason()).isNotNull();
    }

    @Test
    void shouldGetStats() {
        var stats = dlqService.getStatistics();
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalEntries()).isNotNull();
    }
}