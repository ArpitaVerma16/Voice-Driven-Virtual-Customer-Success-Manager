package com.vcsm.controller;

import com.vcsm.model.Complaint;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.service.BlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import java.util.Optional;


@RestController
@RequestMapping("/api/blockchain")
@CrossOrigin(origins = "*")
public class BlockchainController {

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @GetMapping("/verify/{id}")
    public ResponseEntity<Map<String, Object>> verifyComplaint(@PathVariable Long id) {
        Map<String, Object> result = blockchainService.verifyComplaint(id);

        if (!(boolean) result.get("success")) {

        if (!Boolean.TRUE.equals(result.get("success"))) {

            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add-block/{id}")
    public ResponseEntity<?> addBlock(@PathVariable Long id, @RequestParam String action) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(id);
        if (complaintOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BlockchainService.Block block = blockchainService.addBlock(complaintOpt.get(), action);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Block added successfully",
            "blockIndex", block.getIndex(),
            "blockHash", block.getHash()
        ));
    }

    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getBlockchainDetails() {
        return ResponseEntity.ok(blockchainService.getBlockchainDetails());
    }

    @GetMapping("/complaint/{id}/blocks")
    public ResponseEntity<List<BlockchainService.Block>> getComplaintBlocks(@PathVariable Long id) {
        return ResponseEntity.ok(blockchainService.getComplaintBlocks(id));
    }

    @GetMapping("/qr/{id}")
    public ResponseEntity<Map<String, String>> getVerificationQR(@PathVariable Long id) {
        String data = blockchainService.generateVerificationData(id);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "data", data,
            "complaintId", String.valueOf(id)
        ));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateBlockchain() {
        boolean isValid = blockchainService.verifyBlockchainIntegrity();
        return ResponseEntity.ok(Map.of(
            "valid", isValid,
            "message", isValid ? "Blockchain is valid and tamper-proof" : "Blockchain integrity compromised!"
        ));
    }
}