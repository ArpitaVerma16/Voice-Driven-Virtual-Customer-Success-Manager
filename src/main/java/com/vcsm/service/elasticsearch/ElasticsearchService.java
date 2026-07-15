// service/elasticsearch/ElasticsearchService.java
package com.vcsm.service.elasticsearch;

import com.vcsm.model.Complaint;
import com.vcsm.model.elasticsearch.ComplaintDocument;
import com.vcsm.repository.elasticsearch.ComplaintElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {

    private final ComplaintElasticsearchRepository elasticsearchRepository;

    /**
     * Index a single complaint document
     */
    @Transactional
    public void indexComplaint(Complaint complaint) {
        try {
            ComplaintDocument doc = ComplaintDocument.fromComplaint(complaint);
            elasticsearchRepository.save(doc);
            log.info("Successfully indexed complaint ID: {}", complaint.getId());
        } catch (Exception e) {
            log.error("Failed to index complaint ID: {}", complaint.getId(), e);
            throw new RuntimeException("Elasticsearch indexing failed", e);
        }
    }

    /**
     * Index multiple complaints in bulk
     */
    @Transactional
    public void indexComplaintsBulk(List<Complaint> complaints) {
        List<ComplaintDocument> documents = complaints.stream()
            .map(ComplaintDocument::fromComplaint)
            .collect(Collectors.toList());
        
        try {
            elasticsearchRepository.saveAll(documents);
            log.info("Successfully indexed {} complaints", documents.size());
        } catch (Exception e) {
            log.error("Failed to bulk index complaints", e);
            throw new RuntimeException("Bulk indexing failed", e);
        }
    }

    /**
     * Full-text search with fuzzy matching
     */
    public List<ComplaintDocument> fuzzySearch(String query) {
        try {
            return elasticsearchRepository.searchByDescriptionFuzzy(query);
        } catch (Exception e) {
            log.error("Fuzzy search failed for query: {}", query, e);
            return List.of();
        }
    }

    /**
     * Search with filters
     */
    public List<ComplaintDocument> searchWithFilters(String query, String status, String category) {
        try {
            if (status != null && !status.isEmpty()) {
                return elasticsearchRepository.searchByDescriptionAndStatus(query, status);
            }
            return elasticsearchRepository.multiFieldSearch(query);
        } catch (Exception e) {
            log.error("Filtered search failed", e);
            return List.of();
        }
    }

    /**
     * Auto-complete suggestions
     */
    public List<String> getAutoCompleteSuggestions(String prefix) {
        try {
            return elasticsearchRepository.autoCompleteDescription(prefix)
                .stream()
                .map(ComplaintDocument::getDescription)
                .limit(10)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Auto-complete failed for prefix: {}", prefix, e);
            return List.of();
        }
    }

    /**
     * Get complaint statistics
     */
    public Map<String, Long> getComplaintStatistics() {
        try {
            return Map.of(
                "OPEN", elasticsearchRepository.countByStatus("OPEN"),
                "IN_PROGRESS", elasticsearchRepository.countByStatus("IN_PROGRESS"),
                "RESOLVED", elasticsearchRepository.countByStatus("RESOLVED"),
                "CLOSED", elasticsearchRepository.countByStatus("CLOSED")
            );
        } catch (Exception e) {
            log.error("Failed to get complaint statistics", e);
            return Map.of();
        }
    }

    /**
     * Delete complaint from index
     */
    @Transactional
    public void deleteComplaint(Long complaintId) {
        try {
            // Find and delete by complaintId
            elasticsearchRepository.findByComplaintId(complaintId)
                .ifPresent(doc -> {
                    elasticsearchRepository.deleteById(doc.getId());
                    log.info("Deleted complaint ID: {} from Elasticsearch", complaintId);
                });
        } catch (Exception e) {
            log.error("Failed to delete complaint from Elasticsearch: {}", complaintId, e);
        }
    }

    /**
     * Update complaint in index
     */
    @Transactional
    public void updateComplaint(Complaint complaint) {
        deleteComplaint(complaint.getId());
        indexComplaint(complaint);
        log.info("Updated complaint ID: {} in Elasticsearch", complaint.getId());
    }

    /**
     * Reindex all complaints from database
     */
    @Transactional
    public void reindexAll(List<Complaint> complaints) {
        log.info("Starting full reindex of {} complaints", complaints.size());
        
        // Delete existing index
        elasticsearchRepository.deleteAll();
        
        // Bulk index all complaints
        indexComplaintsBulk(complaints);
        
        log.info("Reindexing completed successfully");
    }

    /**
     * Search with pagination
     */
    public ElasticsearchPage<ComplaintDocument> searchWithPagination(
        String query, 
        int page, 
        int size, 
        String sortBy,
        String sortOrder
    ) {
        try {
            // Implementation using Elasticsearch client directly for advanced pagination
            // For simplicity, we'll return all results with offset
            List<ComplaintDocument> results = elasticsearchRepository.multiFieldSearch(query);
            
            int start = page * size;
            int end = Math.min(start + size, results.size());
            
            List<ComplaintDocument> content = results.subList(start, end);
            
            return ElasticsearchPage.<ComplaintDocument>builder()
                .content(content)
                .totalElements((long) results.size())
                .totalPages((int) Math.ceil((double) results.size() / size))
                .pageNumber(page)
                .pageSize(size)
                .build();
        } catch (Exception e) {
            log.error("Pagination search failed", e);
            return ElasticsearchPage.empty();
        }
    }
}
