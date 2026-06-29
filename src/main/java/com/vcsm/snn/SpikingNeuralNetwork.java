package com.vcsm.snn;

import com.vcsm.snn.LIFNeuronModel.LIFNeuron;
import com.vcsm.snn.LIFNeuronModel.LayerResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SpikingNeuralNetwork {

    @Autowired
    private LIFNeuronModel lifNeuronModel;

    @Autowired
    private STDPLearning stdpLearning;

    private List<LIFNeuron> inputLayer;
    private List<LIFNeuron> hiddenLayer;
    private List<LIFNeuron> outputLayer;
    private boolean initialized = false;
    private int inputSize;
    private int hiddenSize;
    private int outputSize;

    /**
     * Initialize SNN architecture
     */
    public SNNConfig initialize(int inputSize, int hiddenSize, int outputSize) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;

        inputLayer = lifNeuronModel.createLayer(inputSize);
        hiddenLayer = lifNeuronModel.createLayer(hiddenSize);
        outputLayer = lifNeuronModel.createLayer(outputSize);

        // Create STDP connections
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                stdpLearning.createConnection("input_" + i, "hidden_" + j, 0.3 + ThreadLocalRandom.current().nextDouble() * 0.4);
            }
        }

        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                stdpLearning.createConnection("hidden_" + i, "output_" + j, 0.3 + ThreadLocalRandom.current().nextDouble() * 0.4);
            }
        }

        initialized = true;

        return new SNNConfig(inputSize, hiddenSize, outputSize, connectionsCount());
    }

    /**
     * Forward pass through SNN
     */
    public SNNResponse forward(double[] input) {
        if (!initialized) {
            throw new RuntimeException("SNN not initialized. Call initialize() first.");
        }

        double timeStep = 1.0;
        int timeSteps = 10;

        // Process input layer
        LayerResponse inputResponse = lifNeuronModel.processLayer(inputLayer, input, timeStep);

        // Process hidden layer
        double[] hiddenInput = inputResponse.getOutputs().stream().mapToDouble(Double::doubleValue).toArray();
        LayerResponse hiddenResponse = lifNeuronModel.processLayer(hiddenLayer, hiddenInput, timeStep);

        // Process output layer
        double[] outputInput = hiddenResponse.getOutputs().stream().mapToDouble(Double::doubleValue).toArray();
        LayerResponse outputResponse = lifNeuronModel.processLayer(outputLayer, outputInput, timeStep);

        // Apply STDP learning
        stdpLearning.updateWeights(
            System.currentTimeMillis(),
            System.currentTimeMillis() + 10,
            timeStep
        );

        return new SNNResponse(
            outputResponse,
            hiddenResponse,
            inputResponse,
            outputResponse.getTotalSpikes(),
            outputResponse.getAveragePotential()
        );
    }

    /**
     * Train SNN using unsupervised learning
     */
    public void train(double[][] trainingData, int epochs) {
        if (!initialized) {
            throw new RuntimeException("SNN not initialized. Call initialize() first.");
        }

        for (int epoch = 0; epoch < epochs; epoch++) {
            for (double[] data : trainingData) {
                forward(data);
            }
        }
    }

    /**
     * Get network stats
     */
    public Map<String, Object> getNetworkStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("initialized", initialized);
        stats.put("inputSize", inputSize);
        stats.put("hiddenSize", hiddenSize);
        stats.put("outputSize", outputSize);
        stats.put("totalConnections", connectionsCount());
        stats.put("status", "Spiking Neural Network active");
        return stats;
    }

    private int connectionsCount() {
        return stdpLearning.getAllConnections().size();
    }

    public static class SNNConfig {
        private final int inputSize;
        private final int hiddenSize;
        private final int outputSize;
        private final int totalConnections;

        public SNNConfig(int inputSize, int hiddenSize, int outputSize, int totalConnections) {
            this.inputSize = inputSize;
            this.hiddenSize = hiddenSize;
            this.outputSize = outputSize;
            this.totalConnections = totalConnections;
        }

        public int getInputSize() { return inputSize; }
        public int getHiddenSize() { return hiddenSize; }
        public int getOutputSize() { return outputSize; }
        public int getTotalConnections() { return totalConnections; }
    }

    public static class SNNResponse {
        private final LayerResponse outputResponse;
        private final LayerResponse hiddenResponse;
        private final LayerResponse inputResponse;
        private final int totalSpikes;
        private final double averagePotential;

        public SNNResponse(LayerResponse outputResponse, LayerResponse hiddenResponse,
                           LayerResponse inputResponse, int totalSpikes, double averagePotential) {
            this.outputResponse = outputResponse;
            this.hiddenResponse = hiddenResponse;
            this.inputResponse = inputResponse;
            this.totalSpikes = totalSpikes;
            this.averagePotential = averagePotential;
        }

        public LayerResponse getOutputResponse() { return outputResponse; }
        public LayerResponse getHiddenResponse() { return hiddenResponse; }
        public LayerResponse getInputResponse() { return inputResponse; }
        public int getTotalSpikes() { return totalSpikes; }
        public double getAveragePotential() { return averagePotential; }
    }
}