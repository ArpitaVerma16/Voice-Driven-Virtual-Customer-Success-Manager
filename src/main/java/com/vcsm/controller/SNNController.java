package com.vcsm.controller;

import com.vcsm.snn.SpikingNeuralNetwork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/snn")
@CrossOrigin(origins = "*")
public class SNNController {

    @Autowired
    private SpikingNeuralNetwork spikingNeuralNetwork;

    @PostMapping("/init")
    public ResponseEntity<SpikingNeuralNetwork.SNNConfig> initialize(
            @RequestParam(defaultValue = "4") int inputSize,
            @RequestParam(defaultValue = "8") int hiddenSize,
            @RequestParam(defaultValue = "2") int outputSize) {
        return ResponseEntity.ok(spikingNeuralNetwork.initialize(inputSize, hiddenSize, outputSize));
    }

    @PostMapping("/forward")
    public ResponseEntity<SpikingNeuralNetwork.SNNResponse> forward(@Valid @RequestBody double[] input) {
        return ResponseEntity.ok(spikingNeuralNetwork.forward(input));
    }

    @PostMapping("/train")
    public ResponseEntity<Map<String, String>> train(@Valid @RequestBody TrainingRequest request) {
        spikingNeuralNetwork.train(request.getData(), request.getEpochs());
        return ResponseEntity.ok(Map.of("status", "success", "message", "Training completed"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(spikingNeuralNetwork.getNetworkStats());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "Spiking Neural Network System active");
        status.put("features", new String[]{
            "LIF Neuron Model",
            "STDP Learning",
            "Event-driven Computation",
            "Temporal Coding",
            "Neuromorphic Computing"
        });
        return ResponseEntity.ok(status);
    }

    public static class TrainingRequest {
        private double[][] data;
        private int epochs = 10;

        public double[][] getData() { return data; }
        public void setData(double[][] data) { this.data = data; }
        public int getEpochs() { return epochs; }
        public void setEpochs(int epochs) { this.epochs = epochs; }
    }
}