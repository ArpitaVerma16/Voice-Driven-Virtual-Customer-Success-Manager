package com.vcsm.duplicate;

import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SimilarityService {

    @Autowired
    private EmbeddingService embeddingService;

    private static final double SIMILARITY_THRESHOLD = 0.85;

    /**
     * Calculate cosine similarity between two vectors
     */
    public double cosineSimilarity(double[] vec1, double[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length == 0 || vec2.length == 0) {
            return 0.0;
        }

        RealVector v1 = new ArrayRealVector(vec1);
        RealVector v2 = new ArrayRealVector(vec2);

        double dotProduct = v1.dotProduct(v2);
        double norm1 = v1.getNorm();
        double norm2 = v2.getNorm();

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    /**
     * Find duplicate complaints
     */
    public DuplicateResult findDuplicates(String newComplaintText, List<Long> existingComplaintIds) {
        double[] newEmbedding = embeddingService.generateEmbedding(newComplaintText);

        List<SimilarityResult> results = new ArrayList<>();

        for (Long complaintId : existingComplaintIds) {
            double[] existingEmbedding = embeddingService.getEmbedding(complaintId);
            if (existingEmbedding != null) {
                double similarity = cosineSimilarity(newEmbedding, existingEmbedding);
                if (similarity >= SIMILARITY_THRESHOLD) {
                    results.add(new SimilarityResult(complaintId, similarity));
                }
            }
        }

        // Sort by similarity (highest first)
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

        return new DuplicateResult(
            results,
            !results.isEmpty(),
            results.isEmpty() ? "No duplicates found" : "Found " + results.size() + " potential duplicates"
        );
    }

    /**
     * Get similarity score between two complaints
     */
    public double getSimilarity(Long complaintId1, Long complaintId2) {
        double[] emb1 = embeddingService.getEmbedding(complaintId1);
        double[] emb2 = embeddingService.getEmbedding(complaintId2);

        if (emb1 == null || emb2 == null) {
            return 0.0;
        }

        return cosineSimilarity(emb1, emb2);
    }

    /**
     * Suggest existing complaints before creating duplicate
     */
    public SuggestionResult suggestExisting(String newComplaintText, List<Long> existingComplaintIds) {
        DuplicateResult result = findDuplicates(newComplaintText, existingComplaintIds);

        List<SuggestedComplaint> suggestions = new ArrayList<>();
        for (SimilarityResult sr : result.getResults()) {
            suggestions.add(new SuggestedComplaint(sr.getComplaintId(), sr.getSimilarity()));
        }

        return new SuggestionResult(
            suggestions,
            result.hasDuplicates(),
            result.getMessage()
        );
    }

    /**
     * Check if two complaints are duplicates
     */
    public boolean areDuplicates(Long complaintId1, Long complaintId2) {
        double similarity = getSimilarity(complaintId1, complaintId2);
        return similarity >= SIMILARITY_THRESHOLD;
    }

    public static class SimilarityResult {
        private final Long complaintId;
        private final double similarity;

        public SimilarityResult(Long complaintId, double similarity) {
            this.complaintId = complaintId;
            this.similarity = similarity;
        }

        public Long getComplaintId() { return complaintId; }
        public double getSimilarity() { return similarity; }
    }

    public static class DuplicateResult {
        private final List<SimilarityResult> results;
        private final boolean hasDuplicates;
        private final String message;

        public DuplicateResult(List<SimilarityResult> results, boolean hasDuplicates, String message) {
            this.results = results;
            this.hasDuplicates = hasDuplicates;
            this.message = message;
        }

        public List<SimilarityResult> getResults() { return results; }
        public boolean hasDuplicates() { return hasDuplicates; }
        public String getMessage() { return message; }
    }

    public static class SuggestionResult {
        private final List<SuggestedComplaint> suggestions;
        private final boolean hasSuggestions;
        private final String message;

        public SuggestionResult(List<SuggestedComplaint> suggestions, boolean hasSuggestions, String message) {
            this.suggestions = suggestions;
            this.hasSuggestions = hasSuggestions;
            this.message = message;
        }

        public List<SuggestedComplaint> getSuggestions() { return suggestions; }
        public boolean isHasSuggestions() { return hasSuggestions; }
        public String getMessage() { return message; }
    }

    public static class SuggestedComplaint {
        private final Long complaintId;
        private final double similarity;

        public SuggestedComplaint(Long complaintId, double similarity) {
            this.complaintId = complaintId;
            this.similarity = similarity;
        }

        public Long getComplaintId() { return complaintId; }
        public double getSimilarity() { return similarity; }
    }
}