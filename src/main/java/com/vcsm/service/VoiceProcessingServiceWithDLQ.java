package com.vcsm.service;

import com.vcsm.model.dlq.DeadLetterQueueEntry;
import com.vcsm.service.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceProcessingServiceWithDLQ {

    private final DeadLetterQueueService dlqService;
    private final VoiceProcessingService voiceProcessingService;

    @PostConstruct
    public void init() {
        // Register retry handler for voice processing
        dlqService.registerRetryHandler("VOICE_PROCESSING", this::processVoiceRetry);
    }

    /**
     * Process voice command with DLQ protection
     */
    public CompletableFuture<String> processVoiceCommand(String transcript, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Processing voice command: {}", transcript);
                String result = voiceProcessingService.processCommand(transcript, userId);
                return result;
            } catch (Exception e) {
                log.error("Voice processing failed, adding to DLQ", e);
                
                // Add to DLQ
                Map<String, Object> payload = Map.of(
                    "transcript", transcript,
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
                );
                
                dlqService.addToDeadLetterQueue(
                    "VOICE_PROCESSING",
                    UUID.randomUUID().toString(),
                    payload,
                    e,
                    userId,
                    "VOICE_COMMAND",
                    null,
                    Map.of("transcript", transcript)
                );
                
                throw new RuntimeException("Voice processing failed, added to DLQ", e);
            }
        });
    }

    /**
     * Retry handler for voice processing
     */
    public Boolean processVoiceRetry(Object payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload;
            String transcript = (String) data.get("transcript");
            String userId = (String) data.get("userId");
            
            log.info("🔄 Retrying voice command: {}", transcript);
            String result = voiceProcessingService.processCommand(transcript, userId);
            return true;
        } catch (Exception e) {
            log.error("Voice retry failed", e);
            return false;
        }
    }
}