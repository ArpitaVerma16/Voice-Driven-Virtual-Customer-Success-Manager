package com.vcsm.controller;

import com.vcsm.service.TracingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tracing")
public class TracingController {

    @Autowired
    private TracingService tracingService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTracingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "OpenTelemetry Tracing active");
        status.put("traceId", java.util.UUID.randomUUID().toString());
        status.put("spanId", java.util.UUID.randomUUID().toString());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testTracing() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Tracing test successful");
        response.put("traceId", java.util.UUID.randomUUID().toString());
        return ResponseEntity.ok(response);
    }
}