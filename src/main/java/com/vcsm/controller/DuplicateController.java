package com.vcsm.controller;

import com.vcsm.duplicate.EmbeddingService;
import com.vcsm.duplicate.SimilarityService;
import com.vcsm.model.Complaint;
import com.vcsm.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/duplicate")
@CrossOrigin(origins = "*")
public class DuplicateController {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private SimilarityService similarityService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @PostMapping("/detect")
    public ResponseEntity<SimilarityService.DuplicateResult> detectDuplicate(@RequestBody DuplicateRequest request) {
        List<Complaint> existingComplaints = complaintRepository.findAll();
        List<Long> existingIds = existingComplaints.stream()
            .map(Complaint::getId)
            .collect(Collectors.toList());

        return ResponseEntity.ok(similarityService.findDuplicates(request.getText(), existingIds));
    }

    @PostMapping("/suggest")
    public ResponseEntity<SimilarityService.SuggestionResult> suggestExisting(@RequestBody DuplicateRequest request) {
        List<Complaint> existingComplaints = complaintRepository.findAll();
        List<Long> existingIds = existingComplaints.stream()
            .map(Complaint::getId)
            .collect(Collectors.toList());

        return ResponseEntity.ok(similarityService.suggestExisting(request.getText(), existingIds));
    }

    @PostMapping("/store/{complaintId}")
    public ResponseEntity<Map<String, String>> storeEmbedding(
            @PathVariable Long complaintId,
            @RequestBody Complaint complaint) {
        embeddingService.storeEmbedding(complaintId, complaint.getDescription());
        return ResponseEntity.ok(Map.of("status", "success", "message", "Embedding stored"));
    }

    @GetMapping("/similarity")
    public ResponseEntity<Map<String, Object>> getSimilarity(
            @RequestParam Long id1,
            @RequestParam Long id2) {
        double similarity = similarityService.getSimilarity(id1, id2);
        Map<String, Object> response = new HashMap<>();
        response.put("complaint1", id1);
        response.put("complaint2", id2);
        response.put("similarity", similarity);
        response.put("isDuplicate", similarity >= 0.85);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEmbeddings", embeddingService.getAllEmbeddings().size());
        stats.put("threshold", 0.85);
        stats.put("status", "Semantic Duplicate Detection active");
        return ResponseEntity.ok(stats);
    }

    public static class DuplicateRequest {
        private String text;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}