package com.vcsm.quantum;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class QuantumCircuitBuilder {

    /**
     * Build a quantum circuit for feature mapping
     */
    public QuantumCircuit buildFeatureMap(int numQubits, int numLayers) {
        QuantumCircuit circuit = new QuantumCircuit(numQubits);

        // Add initial Hadamard gates
        for (int i = 0; i < numQubits; i++) {
            circuit.addGate("H", i);
        }

        // Add entangling layers
        for (int layer = 0; layer < numLayers; layer++) {
            // Rotation gates with random angles
            for (int i = 0; i < numQubits; i++) {
                double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                circuit.addGate("RY", i, angle);
            }

            // CNOT gates for entanglement
            for (int i = 0; i < numQubits - 1; i++) {
                circuit.addGate("CNOT", i, i + 1);
            }
        }

        // Add measurement
        for (int i = 0; i < numQubits; i++) {
            circuit.addGate("MEASURE", i);
        }

        return circuit;
    }

    /**
     * Build a variational quantum circuit
     */
    public QuantumCircuit buildVariationalCircuit(int numQubits, int numLayers, double[] params) {
        QuantumCircuit circuit = new QuantumCircuit(numQubits);

        int paramIndex = 0;
        for (int layer = 0; layer < numLayers; layer++) {
            // Rotations with trainable parameters
            for (int i = 0; i < numQubits; i++) {
                double angle = params.length > paramIndex ? params[paramIndex] : 0.0;
                circuit.addGate("RY", i, angle);
                paramIndex++;
                if (paramIndex >= params.length) paramIndex = 0;
            }

            // Entangling layers
            for (int i = 0; i < numQubits - 1; i++) {
                circuit.addGate("CNOT", i, i + 1);
            }
        }

        return circuit;
    }

    /**
     * Build a quantum kernel circuit
     */
    public QuantumCircuit buildKernelCircuit(double[] dataPoint) {
        int numQubits = dataPoint.length;
        QuantumCircuit circuit = new QuantumCircuit(numQubits);

        // Encode data into quantum states
        for (int i = 0; i < numQubits; i++) {
            circuit.addGate("RY", i, dataPoint[i] * Math.PI);
        }

        // Add entanglement
        for (int i = 0; i < numQubits - 1; i++) {
            circuit.addGate("CNOT", i, i + 1);
        }

        return circuit;
    }

    public static class QuantumCircuit {
        private final int numQubits;
        private final List<QuantumGate> gates = new ArrayList<>();

        public QuantumCircuit(int numQubits) {
            this.numQubits = numQubits;
        }

        public void addGate(String type, int qubit, Object... params) {
            gates.add(new QuantumGate(type, qubit, params));
        }

        public int getNumQubits() { return numQubits; }
        public List<QuantumGate> getGates() { return gates; }

        public String getCircuitString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Circuit with ").append(numQubits).append(" qubits\n");
            for (QuantumGate gate : gates) {
                sb.append("  ").append(gate.toString()).append("\n");
            }
            return sb.toString();
        }
    }

    public static class QuantumGate {
        private final String type;
        private final int qubit;
        private final Object[] params;

        public QuantumGate(String type, int qubit, Object[] params) {
            this.type = type;
            this.qubit = qubit;
            this.params = params;
        }

        public String getType() { return type; }
        public int getQubit() { return qubit; }
        public Object[] getParams() { return params; }

        @Override
        public String toString() {
            if (params.length > 0) {
                return type + " on qubit " + qubit + " with params: " + Arrays.toString(params);
            }
            return type + " on qubit " + qubit;
        }
    }
}