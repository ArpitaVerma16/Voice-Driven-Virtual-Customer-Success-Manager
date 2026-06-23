package com.vcsm.controller;

import com.vcsm.service.LiveDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/live")
@CrossOrigin(origins = "*")
public class LiveDashboardController {

    @Autowired
    private LiveDashboardService liveDashboardService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(60000L); // 60 second timeout

        // Send initial data
        try {
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data(liveDashboardService.getLiveStats()));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // Schedule periodic updates
        var future = scheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> update = liveDashboardService.getRealtimeUpdate();
                emitter.send(SseEmitter.event()
                        .name("update")
                        .data(update));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 5, 5, TimeUnit.SECONDS);

        // Handle completion and timeout - cancel the scheduled task
        emitter.onCompletion(() -> future.cancel(false));
        emitter.onTimeout(() -> {
            future.cancel(false);
            emitter.complete();
        });

        return emitter;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return liveDashboardService.getLiveStats();
    }

    @PostMapping("/connect")
    public Map<String, String> connect(@RequestParam String sessionId, @RequestParam Long userId) {
        liveDashboardService.userConnected(sessionId, userId);
        return Map.of("status", "connected", "activeUsers", String.valueOf(liveDashboardService.getActiveUserCount()));
    }

    @PostMapping("/disconnect")
    public Map<String, String> disconnect(@RequestParam String sessionId) {
        liveDashboardService.userDisconnected(sessionId);
        return Map.of("status", "disconnected", "activeUsers", String.valueOf(liveDashboardService.getActiveUserCount()));
    }
}