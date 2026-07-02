package com.vcsm.controller;

import com.vcsm.quantum.QuantumMLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quantum-ml")
@CrossOrigin(origins = "*")
public class QuantumMLController {

    @Autowired
    private QuantumMLService quantumMLService;

    @PostMapping("/init")
    public ResponseEntity<QuantumMLService.QuantumModel> initializeModel() {
        return ResponseEntity.ok(quantumMLService.initializeModel());
    }

    @PostMapping("/feature-map")
    public ResponseEntity<double[]> featureMap(@Valid @RequestBody double[] data) {
        return ResponseEntity.ok(quantumMLService.quantumFeatureMap(data));
    }

    @PostMapping("/kernel")
    public ResponseEntity<Double> kernel(@Valid @RequestBody KernelRequest request) {
        return ResponseEntity.ok(quantumMLService.quantumKernel(request.getX1(), request.getX2()));
    }

    @PostMapping("/infer")
    public ResponseEntity<QuantumMLService.QuantumInferenceResult> infer(@Valid @RequestBody double[] input) {
        return ResponseEntity.ok(quantumMLService.quantumInference(input));
    }

    @PostMapping("/train")
    public ResponseEntity<QuantumMLService.QuantumModel> train(@Valid @RequestBody TrainingRequest request) {
        return ResponseEntity.ok(quantumMLService.trainModel(request.getData(), request.getLabels()));
    }

    @PostMapping("/hybrid-predict")
    public ResponseEntity<Double> hybridPredict(@Valid @RequestBody double[] input) {
        return ResponseEntity.ok(quantumMLService.hybridPredict(input));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(quantumMLService.getQuantumStats());
    }

    public static class KernelRequest {
        private double[] x1;
        private double[] x2;

        public double[] getX1() { return x1; }
        public void setX1(double[] x1) { this.x1 = x1; }
        public double[] getX2() { return x2; }
        public void setX2(double[] x2) { this.x2 = x2; }
    }

    public static class TrainingRequest {
        private List<double[]> data;
        private List<Double> labels;

        public List<double[]> getData() { return data; }
        public void setData(List<double[]> data) { this.data = data; }
        public List<Double> getLabels() { return labels; }
        public void setLabels(List<Double> labels) { this.labels = labels; }
    }
}
