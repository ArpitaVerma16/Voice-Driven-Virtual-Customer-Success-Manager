package com.vcsm.controller;

import com.vcsm.service.BulkComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints/bulk")
@CrossOrigin(origins = "*")
public class BulkComplaintController {

    @Autowired
    private BulkComplaintService bulkComplaintService;

    @PostMapping("/resolve")
    public ResponseEntity<Map<String, Object>> bulkResolve(
            @Valid @RequestBody Map<String, Object> request) {
        
        List<Long> complaintIds = (List<Long>) request.get("complaintIds");
        String resolutionNotes = (String) request.get("resolutionNotes");
        
        if (complaintIds == null || complaintIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Please select at least one complaint"
            ));
        }

        Map<String, Object> result = bulkComplaintService.bulkResolve(complaintIds, resolutionNotes);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> bulkUpdateStatus(
            @Valid @RequestBody Map<String, Object> request) {
        
        List<Long> complaintIds = (List<Long>) request.get("complaintIds");
        String newStatus = (String) request.get("status");
        
        if (complaintIds == null || complaintIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Please select at least one complaint"
            ));
        }

        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Please select a status"
            ));
        }

        Map<String, Object> result = bulkComplaintService.bulkUpdateStatus(complaintIds, newStatus);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getAllIds() {
        return ResponseEntity.ok(bulkComplaintService.getAllComplaintIds());
    }

    @GetMapping("/ids/status/{status}")
    public ResponseEntity<List<Long>> getIdsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(bulkComplaintService.getComplaintIdsByStatus(status));
    }
}
