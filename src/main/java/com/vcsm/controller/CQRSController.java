package com.vcsm.controller;

import com.vcsm.cqrs.command.ComplaintCommand;
import com.vcsm.cqrs.query.ComplaintQuery;
import com.vcsm.model.Complaint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cqrs")
@CrossOrigin(origins = "*")
public class CQRSController {

    @Autowired
    private ComplaintCommand complaintCommand;

    @Autowired
    private ComplaintQuery complaintQuery;

    // Write endpoints (Command)
    @PostMapping("/create")
    public ResponseEntity<Complaint> createComplaint(@RequestBody Complaint complaint) {
        return ResponseEntity.ok(complaintCommand.createComplaint(complaint));
    }

    @PutMapping("/update-status/{id}")
    public ResponseEntity<Complaint> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(complaintCommand.updateStatus(id, status));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable Long id) {
        complaintCommand.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update-priority/{id}")
    public ResponseEntity<Complaint> updatePriority(@PathVariable Long id, @RequestParam String priority) {
        return ResponseEntity.ok(complaintCommand.updatePriority(id, priority));
    }

    // Read endpoints (Query)
    @GetMapping("/all")
    public ResponseEntity<List<ComplaintQuery.ComplaintReadModel>> getAllComplaints() {
        return ResponseEntity.ok(complaintQuery.getAllComplaints());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintQuery.ComplaintReadModel> getComplaintById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintQuery.getComplaintById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ComplaintQuery.ComplaintReadModel>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(complaintQuery.getComplaintsByStatus(status));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ComplaintQuery.ComplaintReadModel>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(complaintQuery.getComplaintsByCategory(category));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<ComplaintQuery.ComplaintReadModel>> getByPriority(@PathVariable String priority) {
        return ResponseEntity.ok(complaintQuery.getComplaintsByPriority(priority));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ComplaintQuery.ComplaintAnalytics> getAnalytics() {
        return ResponseEntity.ok(complaintQuery.getAnalytics());
    }
}