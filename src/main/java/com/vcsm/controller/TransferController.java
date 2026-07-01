package com.vcsm.controller;

import com.vcsm.transfer.DomainAdapter;
import com.vcsm.transfer.FederatedTransferService;
import com.vcsm.transfer.KnowledgeTransfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transfer")
@CrossOrigin(origins = "*")
public class TransferController {

    @Autowired
    private FederatedTransferService federatedTransferService;

    @Autowired
    private DomainAdapter domainAdapter;

    @Autowired
    private KnowledgeTransfer knowledgeTransfer;

    @PostMapping("/node/register")
    public ResponseEntity<FederatedTransferService.FederatedNode> registerNode(
            @RequestParam String nodeId,
            @RequestParam String domain,
            @RequestBody double[] domainFeatures) {
        return ResponseEntity.ok(federatedTransferService.registerNode(nodeId, domain, domainFeatures));
    }

    @PostMapping("/start")
    public ResponseEntity<FederatedTransferService.TransferRound> startTransfer(
            @RequestParam String sourceNodeId,
            @RequestParam String targetNodeId,
            @RequestParam(defaultValue = "20") int numSamples) {
        return ResponseEntity.ok(federatedTransferService.startTransferRound(
            sourceNodeId, targetNodeId, numSamples
        ));
    }

    @PostMapping("/adaptive")
    public ResponseEntity<FederatedTransferService.AdaptiveTransferResult> adaptiveTransfer(
            @RequestParam String sourceNodeId,
            @RequestParam String targetNodeId) {
        return ResponseEntity.ok(federatedTransferService.adaptiveTransfer(
            sourceNodeId, targetNodeId
        ));
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<FederatedTransferService.FederatedNode>> getAllNodes() {
        return ResponseEntity.ok(federatedTransferService.getAllNodes());
    }

    @GetMapping("/nodes/{nodeId}")
    public ResponseEntity<FederatedTransferService.FederatedNode> getNode(@PathVariable String nodeId) {
        return ResponseEntity.ok(federatedTransferService.getNode(nodeId));
    }

    @GetMapping("/rounds/{nodeId}")
    public ResponseEntity<List<FederatedTransferService.TransferRound>> getTransferRounds(@PathVariable String nodeId) {
        return ResponseEntity.ok(federatedTransferService.getTransferRounds(nodeId));
    }

    @GetMapping("/similarity")
    public ResponseEntity<Map<String, Double>> getSimilarity(
            @RequestParam String domain1,
            @RequestParam String domain2) {
        Map<String, Double> response = new HashMap<>();
        response.put("similarity", domainAdapter.getDomainSimilarity(domain1, domain2));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/knowledge/create")
    public ResponseEntity<KnowledgeTransfer.KnowledgeBase> createKnowledgeBase(
            @RequestParam String domain,
            @RequestBody KnowledgeRequest request) {
        return ResponseEntity.ok(knowledgeTransfer.createKnowledgeBase(
            domain, request.getData(), request.getLabels()
        ));
    }

    @GetMapping("/knowledge/{domain}")
    public ResponseEntity<KnowledgeTransfer.KnowledgeBase> getKnowledgeBase(@PathVariable String domain) {
        return ResponseEntity.ok(knowledgeTransfer.getKnowledgeBase(domain));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(federatedTransferService.getTransferStats());
    }

    public static class KnowledgeRequest {
        private double[][] data;
        private double[] labels;

        public double[][] getData() { return data; }
        public void setData(double[][] data) { this.data = data; }
        public double[] getLabels() { return labels; }
        public void setLabels(double[] labels) { this.labels = labels; }
    }
}