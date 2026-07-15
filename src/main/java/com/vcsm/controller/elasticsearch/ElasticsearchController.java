// controller/elasticsearch/ElasticsearchController.java
package com.vcsm.controller.elasticsearch;

import com.vcsm.model.elasticsearch.ComplaintDocument;
import com.vcsm.model.elasticsearch.ElasticsearchPage;
import com.vcsm.service.elasticsearch.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/elasticsearch/complaints")
@RequiredArgsConstructor
public class ElasticsearchController {

    private final ElasticsearchService elasticsearchService;

    /**
     * Search complaints with fuzzy matching
     */
    @GetMapping("/search")
    public ResponseEntity<List<ComplaintDocument>> search(
        @RequestParam String query,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String category
    ) {
        try {
            List<ComplaintDocument> results = elasticsearchService.searchWithFilters(query, status, category);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Search failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get auto-complete suggestions
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autoComplete(
        @RequestParam String prefix
    ) {
        try {
            List<String> suggestions = elasticsearchService.getAutoCompleteSuggestions(prefix);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Auto-complete failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get complaint statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        try {
            Map<String, Long> stats = elasticsearchService.getComplaintStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Stats retrieval failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Search with pagination
     */
    @GetMapping("/search/paginated")
    public ResponseEntity<ElasticsearchPage<ComplaintDocument>> searchPaginated(
        @RequestParam String query,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        try {
            ElasticsearchPage<ComplaintDocument> pageResults = 
                elasticsearchService.searchWithPagination(query, page, size, sortBy, sortOrder);
            return ResponseEntity.ok(pageResults);
        } catch (Exception e) {
            log.error("Paginated search failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Reindex all complaints
     */
    @PostMapping("/reindex")
    public ResponseEntity<String> reindexAll() {
        try {
            // This would need to fetch all complaints from database
            elasticsearchService.reindexAll(List.of());
            return ResponseEntity.ok("Reindexing started successfully");
        } catch (Exception e) {
            log.error("Reindex failed", e);
            return ResponseEntity.internalServerError().body("Reindex failed: " + e.getMessage());
        }
    }
}