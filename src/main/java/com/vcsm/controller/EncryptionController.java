package com.vcsm.controller;

import com.vcsm.encryption.HomomorphicEncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/encryption")
@lombok.RequiredArgsConstructor
public class EncryptionController {

    private final HomomorphicEncryptionService encryptionService;

    @PostMapping("/keys/generate")
    public ResponseEntity<HomomorphicEncryptionService.KeyPair> generateKeys(@RequestParam String userId) {
        return ResponseEntity.ok(encryptionService.generateKeyPair(userId));
    }

    @PostMapping("/encrypt")
    public ResponseEntity<HomomorphicEncryptionService.EncryptedData> encrypt(
            @RequestParam String userId,
            @Valid @RequestBody double[] data) {
        return ResponseEntity.ok(encryptionService.encrypt(userId, data));
    }

    @PostMapping("/decrypt")
    public ResponseEntity<double[]> decrypt(
            @RequestParam String userId,
            @Valid @RequestBody HomomorphicEncryptionService.EncryptedData encryptedData) {
        return ResponseEntity.ok(encryptionService.decrypt(userId, encryptedData));
    }

    @PostMapping("/add")
    public ResponseEntity<HomomorphicEncryptionService.EncryptedData> add(
            @Valid @RequestBody AddRequest request) {
        return ResponseEntity.ok(encryptionService.encryptedAdd(request.getA(), request.getB()));
    }

    @PostMapping("/multiply")
    public ResponseEntity<HomomorphicEncryptionService.EncryptedData> multiply(
            @Valid @RequestBody AddRequest request) {
        return ResponseEntity.ok(encryptionService.encryptedMultiply(request.getA(), request.getB()));
    }

    @PostMapping("/predict")
    public ResponseEntity<HomomorphicEncryptionService.EncryptedPrediction> predict(
            @RequestParam String userId,
            @Valid @RequestBody PredictRequest request) {
        return ResponseEntity.ok(encryptionService.encryptedPredict(userId, request.getInput(), request.getModel()));
    }

    @PostMapping("/zk-proof")
    public ResponseEntity<HomomorphicEncryptionService.ZeroKnowledgeProof> generateZKProof(
            @RequestParam String userId,
            @Valid @RequestBody double[] data) {
        return ResponseEntity.ok(encryptionService.generateZKProof(userId, data));
    }

    @PostMapping("/zk-verify")
    public ResponseEntity<Map<String, Boolean>> verifyZKProof(
            @Valid @RequestBody HomomorphicEncryptionService.ZeroKnowledgeProof proof) {
        return ResponseEntity.ok(Map.of("verified", encryptionService.verifyZKProof(proof)));
    }

    @PostMapping("/aggregate")
    public ResponseEntity<HomomorphicEncryptionService.EncryptedData> aggregate(
            @Valid @RequestBody List<HomomorphicEncryptionService.EncryptedData> encryptedDataList) {
        return ResponseEntity.ok(encryptionService.secureAggregate(encryptedDataList));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(encryptionService.getEncryptionStats());
    }

    public static class AddRequest {
        private HomomorphicEncryptionService.EncryptedData a;
        private HomomorphicEncryptionService.EncryptedData b;

        public HomomorphicEncryptionService.EncryptedData getA() { return a; }
        public void setA(HomomorphicEncryptionService.EncryptedData a) { this.a = a; }
        public HomomorphicEncryptionService.EncryptedData getB() { return b; }
        public void setB(HomomorphicEncryptionService.EncryptedData b) { this.b = b; }
    }

    public static class PredictRequest {
        private double[] input;
        private double[] model;

        public double[] getInput() { return input; }
        public void setInput(double[] input) { this.input = input; }
        public double[] getModel() { return model; }
        public void setModel(double[] model) { this.model = model; }
    }
}
