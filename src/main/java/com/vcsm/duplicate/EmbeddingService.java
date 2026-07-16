package com.vcsm.duplicate;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EmbeddingService {

    private final Map<Long, double[]> complaintEmbeddings = new ConcurrentHashMap<>();
    private final Map<String, double[]> wordEmbeddings = new ConcurrentHashMap<>();

    private static final int EMBEDDING_SIZE = 128;

    public EmbeddingService() {
        initializeWordEmbeddings();
    }

    private void initializeWordEmbeddings() {
        // Simulate word embeddings for common words
        String[] commonWords = {"water", "leak", "pipe", "noise", "loud", "music", 
            "maintenance", "security", "parking", "clean", "garbage", "electricity"};

        for (String word : commonWords) {
            double[] embedding = new double[EMBEDDING_SIZE];
            for (int i = 0; i < EMBEDDING_SIZE; i++) {
                embedding[i] = ThreadLocalRandom.current().nextGaussian() * 0.1;
            }
            wordEmbeddings.put(word, embedding);
        }
    }

    /**
     * Generate sentence embedding
     */
    public double[] generateEmbedding(String text) {
        if (text == null || text.isEmpty()) {
            return new double[EMBEDDING_SIZE];
        }

        String[] words = text.toLowerCase().replaceAll("[^a-zA-Z ]", "").split("\\s+");
        double[] sentenceEmbedding = new double[EMBEDDING_SIZE];

        int wordCount = 0;
        for (String word : words) {
            double[] wordVec = wordEmbeddings.getOrDefault(word, getRandomEmbedding());
            for (int i = 0; i < EMBEDDING_SIZE; i++) {
                sentenceEmbedding[i] += wordVec[i];
            }
            wordCount++;
        }

        if (wordCount > 0) {
            for (int i = 0; i < EMBEDDING_SIZE; i++) {
                sentenceEmbedding[i] /= wordCount;
            }
        }

        // Normalize
        normalize(sentenceEmbedding);

        return sentenceEmbedding;
    }

    private double[] getRandomEmbedding() {
        double[] vec = new double[EMBEDDING_SIZE];
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            vec[i] = ThreadLocalRandom.current().nextGaussian() * 0.05;
        }
        return vec;
    }

    private void normalize(double[] vector) {
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    /**
     * Store embedding for a complaint
     */
    public void storeEmbedding(Long complaintId, String text) {
        double[] embedding = generateEmbedding(text);
        complaintEmbeddings.put(complaintId, embedding);
    }

    /**
     * Get embedding for a complaint
     */
    public double[] getEmbedding(Long complaintId) {
        return complaintEmbeddings.get(complaintId);
    }

    /**
     * Get all embeddings
     */
    public Map<Long, double[]> getAllEmbeddings() {
        return new HashMap<>(complaintEmbeddings);
    }

    /**
     * Clear all embeddings
     */
    public void clearEmbeddings() {
        complaintEmbeddings.clear();
    }
}