package com.vcsm.ml;

import opennlp.tools.doccat.*;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Component
public class ComplaintClassifier {

    private DoccatModel model;
    private DocumentCategorizerME categorizer;
    private final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

    // Training data with categories and sample phrases
    private final Map<String, List<String>> trainingData = new HashMap<>();

    @PostConstruct
    public void init() {
        loadTrainingData();
        trainModel();
    }

    private void loadTrainingData() {
        // NOISE
        trainingData.put("NOISE", Arrays.asList(
            "loud music from neighbor",
            "construction noise",
            "party noise after midnight",
            "dogs barking continuously",
            "noise from renovation"
        ));

        // MAINTENANCE
        trainingData.put("MAINTENANCE", Arrays.asList(
            "pipe burst in kitchen",
            "water leak from ceiling",
            "AC not cooling",
            "heater not working",
            "power outage",
            "door lock broken",
            "plumbing issue"
        ));

        // SECURITY
        trainingData.put("SECURITY", Arrays.asList(
            "suspicious person in building",
            "break-in attempt",
            "CCTV not working",
            "theft in common area",
            "unauthorized entry"
        ));

        // CLEANLINESS
        trainingData.put("CLEANLINESS", Arrays.asList(
            "garbage not collected",
            "common area unclean",
            "staircase not swept",
            "pest infestation",
            "bad smell in corridor"
        ));

        // PARKING
        trainingData.put("PARKING", Arrays.asList(
            "car parked in wrong spot",
            "visitor taking resident parking",
            "blocked driveway",
            "parking space insufficient"
        ));

        // UTILITIES
        trainingData.put("UTILITIES", Arrays.asList(
            "water supply disruption",
            "electricity fluctuation",
            "internet not working",
            "gas leakage"
        ));

        // OTHER
        trainingData.put("OTHER", Arrays.asList(
            "general query",
            "suggestion for improvement",
            "feedback about service",
            "information request"
        ));
    }

    private void trainModel() {
        try {
            List<DocumentSample> samples = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : trainingData.entrySet()) {
                String category = entry.getKey();
                for (String text : entry.getValue()) {
                    String[] tokens = tokenizer.tokenize(text);
                    samples.add(new DocumentSample(category, tokens));
                }
            }

            TrainingParameters params = TrainingParameters.defaultParams();
            model = DocumentCategorizerME.train(
                new CollectionObjectStream<>(samples),
                params
            );
            categorizer = new DocumentCategorizerME(model);

            System.out.println("✅ Complaint classifier trained with " + samples.size() + " samples");

        } catch (IOException e) {
            System.err.println("❌ Failed to train classification model: " + e.getMessage());
        }
    }

    public ClassificationResult classify(String text) {
        if (categorizer == null || text == null || text.isEmpty()) {
            return new ClassificationResult("OTHER", 0.0);
        }

        String[] tokens = tokenizer.tokenize(text);
        double[] outcomes = categorizer.categorize(tokens);
        String category = categorizer.getBestCategory(outcomes);
        double confidence = outcomes[categorizer.getIndex(category)];

        return new ClassificationResult(category, confidence);
    }

    public Map<String, Double> getAllScores(String text) {
        if (categorizer == null || text == null || text.isEmpty()) {
            return new HashMap<>();
        }

        String[] tokens = tokenizer.tokenize(text);
        double[] outcomes = categorizer.categorize(tokens);
        String[] categories = categorizer.getCategories();

        Map<String, Double> scores = new LinkedHashMap<>();
        for (int i = 0; i < categories.length; i++) {
            scores.put(categories[i], outcomes[i]);
        }
        return scores;
    }

    public static class ClassificationResult {
        private final String category;
        private final double confidence;

        public ClassificationResult(String category, double confidence) {
            this.category = category;
            this.confidence = confidence;
        }

        public String getCategory() { return category; }
        public double getConfidence() { return confidence; }
    }

    private static class CollectionObjectStream<T> implements ObjectStream<T> {
        private final Iterator<T> iterator;

        public CollectionObjectStream(Collection<T> collection) {
            this.iterator = collection.iterator();
        }

        @Override
        public T read() throws IOException {
            return iterator.hasNext() ? iterator.next() : null;
        }

        @Override
        public void reset() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {}
    }
}