package com.vcsm.snn;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LIFNeuronModel {

    private static final double V_REST = -70.0; // mV
    private static final double V_THRESHOLD = -55.0; // mV
    private static final double V_RESET = -80.0; // mV
    private static final double TAU_M = 10.0; // ms
    private static final double TAU_REF = 2.0; // ms

    /**
     * Simulate a single LIF neuron
     */
    public LIFNeuron createNeuron() {
        return new LIFNeuron(V_REST, V_THRESHOLD, V_RESET, TAU_M, TAU_REF);
    }

    /**
     * Create a layer of LIF neurons
     */
    public List<LIFNeuron> createLayer(int numNeurons) {
        List<LIFNeuron> layer = new ArrayList<>();
        for (int i = 0; i < numNeurons; i++) {
            layer.add(createNeuron());
        }
        return layer;
    }

    /**
     * Process input through neuron layer
     */
    public LayerResponse processLayer(List<LIFNeuron> layer, double[] input, double timeStep) {
        List<Double> outputs = new ArrayList<>();
        List<Integer> spikeTimes = new ArrayList<>();

        for (LIFNeuron neuron : layer) {
            double inputCurrent = ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0;
            neuron.step(inputCurrent, timeStep);
            outputs.add(neuron.getMembranePotential());
            if (neuron.didSpike()) {
                spikeTimes.add(neuron.getSpikeCount());
            }
        }

        return new LayerResponse(outputs, spikeTimes, layer.size());
    }

    public static class LIFNeuron {
        private double membranePotential;
        private final double restingPotential;
        private final double threshold;
        private final double resetPotential;
        private final double tauM;
        private final double tauRef;
        private double refractoryTime = 0;
        private int spikeCount = 0;
        private boolean spiked = false;

        public LIFNeuron(double restingPotential, double threshold, double resetPotential, 
                         double tauM, double tauRef) {
            this.restingPotential = restingPotential;
            this.threshold = threshold;
            this.resetPotential = resetPotential;
            this.tauM = tauM;
            this.tauRef = tauRef;
            this.membranePotential = restingPotential;
        }

        public void step(double inputCurrent, double dt) {
            // Refractory period
            if (refractoryTime > 0) {
                refractoryTime -= dt;
                spiked = false;
                return;
            }

            // Update membrane potential (LIF dynamics)
            double dV = dt / tauM * (restingPotential - membranePotential + inputCurrent);
            membranePotential += dV;

            // Check for spike
            if (membranePotential >= threshold) {
                // Fire spike
                membranePotential = resetPotential;
                refractoryTime = tauRef;
                spikeCount++;
                spiked = true;
            } else {
                spiked = false;
            }
        }

        public double getMembranePotential() { return membranePotential; }
        public boolean didSpike() { return spiked; }
        public int getSpikeCount() { return spikeCount; }
        public void reset() {
            membranePotential = restingPotential;
            refractoryTime = 0;
            spikeCount = 0;
            spiked = false;
        }
    }

    public static class LayerResponse {
        private final List<Double> outputs;
        private final List<Integer> spikeTimes;
        private final int neuronCount;

        public LayerResponse(List<Double> outputs, List<Integer> spikeTimes, int neuronCount) {
            this.outputs = outputs;
            this.spikeTimes = spikeTimes;
            this.neuronCount = neuronCount;
        }

        public List<Double> getOutputs() { return outputs; }
        public List<Integer> getSpikeTimes() { return spikeTimes; }
        public int getNeuronCount() { return neuronCount; }
        public double getAveragePotential() {
            return outputs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }
        public int getTotalSpikes() { return spikeTimes.size(); }
    }
}