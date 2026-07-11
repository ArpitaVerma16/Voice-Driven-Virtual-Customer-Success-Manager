package com.vcsm.enas;

import java.util.*;

public class NeuralArchitecture {
    private String id;
    private List<Integer> layerSizes;
    private List<String> activationFunctions;
    private double accuracy;
    private double complexity;
    private double inferenceTime;
    private int parameters;

    public NeuralArchitecture() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.layerSizes = new ArrayList<>();
        this.activationFunctions = new ArrayList<>();
    }

    public NeuralArchitecture(List<Integer> layerSizes, List<String> activationFunctions) {
        this();
        this.layerSizes = layerSizes;
        this.activationFunctions = activationFunctions;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Integer> getLayerSizes() { return layerSizes; }
    public void setLayerSizes(List<Integer> layerSizes) { this.layerSizes = layerSizes; }

    public List<String> getActivationFunctions() { return activationFunctions; }
    public void setActivationFunctions(List<String> activationFunctions) { this.activationFunctions = activationFunctions; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public double getComplexity() { return complexity; }
    public void setComplexity(double complexity) { this.complexity = complexity; }

    public double getInferenceTime() { return inferenceTime; }
    public void setInferenceTime(double inferenceTime) { this.inferenceTime = inferenceTime; }

    public int getParameters() { return parameters; }
    public void setParameters(int parameters) { this.parameters = parameters; }

    public double getFitness() {
        // Multi-objective fitness function
        double accuracyWeight = 0.6;
        double complexityWeight = 0.2;
        double timeWeight = 0.2;
        
        return (accuracy * accuracyWeight) - (complexity * complexityWeight) - (inferenceTime * timeWeight);
    }

    public NeuralArchitecture mutate() {
        NeuralArchitecture mutant = new NeuralArchitecture(
            new ArrayList<>(this.layerSizes),
            new ArrayList<>(this.activationFunctions)
        );
        
        Random rand = new Random();
        int mutationType = rand.nextInt(3);
        
        switch (mutationType) {
            case 0:
                // Change layer size
                int layerIdx = rand.nextInt(mutant.layerSizes.size());
                int currentSize = mutant.layerSizes.get(layerIdx);
                mutant.layerSizes.set(layerIdx, currentSize + rand.nextInt(20) - 10);
                break;
            case 1:
                // Add layer
                if (mutant.layerSizes.size() < 10) {
                    int newSize = 16 + rand.nextInt(48);
                    mutant.layerSizes.add(newSize);
                    mutant.activationFunctions.add("RELU");
                }
                break;
            case 2:
                // Remove layer
                if (mutant.layerSizes.size() > 2) {
                    int removeIdx = rand.nextInt(mutant.layerSizes.size() - 1) + 1;
                    mutant.layerSizes.remove(removeIdx);
                    mutant.activationFunctions.remove(removeIdx - 1);
                }
                break;
        }
        
        return mutant;
    }

    public NeuralArchitecture crossover(NeuralArchitecture parent2) {
        NeuralArchitecture child = new NeuralArchitecture();
        
        // Crossover layer sizes
        int crossoverPoint = Math.min(
            this.layerSizes.size() / 2,
            parent2.layerSizes.size() / 2
        );
        
        // First half from parent1, second half from parent2
        for (int i = 0; i < crossoverPoint && i < this.layerSizes.size(); i++) {
            child.layerSizes.add(this.layerSizes.get(i));
            child.activationFunctions.add(this.activationFunctions.get(i));
        }
        
        for (int i = crossoverPoint; i < parent2.layerSizes.size(); i++) {
            child.layerSizes.add(parent2.layerSizes.get(i));
            child.activationFunctions.add(parent2.activationFunctions.get(i));
        }
        
        return child;
    }
}