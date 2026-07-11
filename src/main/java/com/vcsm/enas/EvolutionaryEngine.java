package com.vcsm.enas;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class EvolutionaryEngine {

    private static final int POPULATION_SIZE = 20;
    private static final int GENERATIONS = 50;
    private static final double MUTATION_RATE = 0.3;
    private static final double CROSSOVER_RATE = 0.7;
    private static final int ELITE_COUNT = 2;

    /**
     * Initialize population with random architectures
     */
    public List<NeuralArchitecture> initializePopulation() {
        List<NeuralArchitecture> population = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            int numLayers = 2 + rand.nextInt(4);
            List<Integer> layerSizes = new ArrayList<>();
            List<String> activations = new ArrayList<>();

            layerSizes.add(rand.nextInt(32) + 8);
            for (int j = 0; j < numLayers - 1; j++) {
                layerSizes.add(rand.nextInt(64) + 16);
                activations.add(rand.nextBoolean() ? "RELU" : "TANH");
            }
            layerSizes.add(rand.nextInt(8) + 2);

            NeuralArchitecture arch = new NeuralArchitecture(layerSizes, activations);
            arch = evaluateArchitecture(arch);
            population.add(arch);
        }

        return population;
    }

    /**
     * Evaluate architecture (simulated)
     */
    public NeuralArchitecture evaluateArchitecture(NeuralArchitecture arch) {
        // Simulated evaluation
        Random rand = new Random();
        
        // Base accuracy depends on architecture complexity
        int numLayers = arch.getLayerSizes().size();
        double baseAccuracy = 0.5 + Math.min(0.4, numLayers * 0.05);
        
        // Add randomness
        double accuracy = baseAccuracy + (rand.nextDouble() * 0.2) - 0.1;
        accuracy = Math.max(0.3, Math.min(0.98, accuracy));
        
        // Complexity (more layers = higher complexity)
        double complexity = numLayers * 0.1 + (arch.getLayerSizes().stream()
            .mapToInt(Integer::intValue)
            .sum() / 100.0);
        
        // Inference time
        double inferenceTime = numLayers * 2.0 + 
            (arch.getLayerSizes().stream().mapToInt(Integer::intValue).sum() / 50.0);
        
        // Parameters count
        int params = 0;
        for (int i = 0; i < arch.getLayerSizes().size() - 1; i++) {
            params += arch.getLayerSizes().get(i) * arch.getLayerSizes().get(i + 1);
        }
        
        arch.setAccuracy(accuracy);
        arch.setComplexity(complexity);
        arch.setInferenceTime(inferenceTime);
        arch.setParameters(params);
        
        return arch;
    }

    /**
     * Perform selection using tournament selection
     */
    public NeuralArchitecture tournamentSelect(List<NeuralArchitecture> population) {
        Random rand = new Random();
        int tournamentSize = 3;
        NeuralArchitecture best = population.get(rand.nextInt(population.size()));
        
        for (int i = 1; i < tournamentSize; i++) {
            NeuralArchitecture candidate = population.get(rand.nextInt(population.size()));
            if (candidate.getFitness() > best.getFitness()) {
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Run evolutionary search
     */
    public EvolutionResult runEvolution() {
        List<NeuralArchitecture> population = initializePopulation();
        List<Double> bestFitnessHistory = new ArrayList<>();
        NeuralArchitecture bestArchitecture = null;
        double bestFitness = -Double.MAX_VALUE;

        for (int generation = 0; generation < GENERATIONS; generation++) {
            // Evaluate population
            for (NeuralArchitecture arch : population) {
                if (arch.getAccuracy() == 0) {
                    evaluateArchitecture(arch);
                }
            }

            // Find best
            for (NeuralArchitecture arch : population) {
                if (arch.getFitness() > bestFitness) {
                    bestFitness = arch.getFitness();
                    bestArchitecture = arch;
                }
            }

            bestFitnessHistory.add(bestFitness);

            // Select elites
            population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
            List<NeuralArchitecture> nextGeneration = new ArrayList<>();
            
            for (int i = 0; i < ELITE_COUNT && i < population.size(); i++) {
                nextGeneration.add(population.get(i));
            }

            // Create offspring
            Random rand = new Random();
            while (nextGeneration.size() < POPULATION_SIZE) {
                NeuralArchitecture parent1 = tournamentSelect(population);
                NeuralArchitecture parent2 = tournamentSelect(population);

                NeuralArchitecture child;
                if (rand.nextDouble() < CROSSOVER_RATE) {
                    child = parent1.crossover(parent2);
                } else {
                    child = new NeuralArchitecture(
                        new ArrayList<>(parent1.getLayerSizes()),
                        new ArrayList<>(parent1.getActivationFunctions())
                    );
                }

                if (rand.nextDouble() < MUTATION_RATE) {
                    child = child.mutate();
                }

                child = evaluateArchitecture(child);
                nextGeneration.add(child);
            }

            population = nextGeneration;
        }

        return new EvolutionResult(
            bestArchitecture,
            bestFitness,
            GENERATIONS,
            bestFitnessHistory,
            population
        );
    }

    public static class EvolutionResult {
        private final NeuralArchitecture bestArchitecture;
        private final double bestFitness;
        private final int generations;
        private final List<Double> fitnessHistory;
        private final List<NeuralArchitecture> finalPopulation;

        public EvolutionResult(NeuralArchitecture bestArchitecture, double bestFitness,
                              int generations, List<Double> fitnessHistory,
                              List<NeuralArchitecture> finalPopulation) {
            this.bestArchitecture = bestArchitecture;
            this.bestFitness = bestFitness;
            this.generations = generations;
            this.fitnessHistory = fitnessHistory;
            this.finalPopulation = finalPopulation;
        }

        public NeuralArchitecture getBestArchitecture() { return bestArchitecture; }
        public double getBestFitness() { return bestFitness; }
        public int getGenerations() { return generations; }
        public List<Double> getFitnessHistory() { return fitnessHistory; }
        public List<NeuralArchitecture> getFinalPopulation() { return finalPopulation; }
    }
}