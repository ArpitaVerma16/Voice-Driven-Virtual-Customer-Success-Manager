package com.vcsm.controller;

import com.vcsm.model.SmartContract;
import com.vcsm.service.SmartContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
@lombok.RequiredArgsConstructor
public class SmartContractController {

    private final SmartContractService smartContractService;

    @PostMapping
    public ResponseEntity<SmartContract> createContract(@Valid @RequestBody SmartContract contract) {
        return ResponseEntity.ok(smartContractService.createContract(contract));
    }

    @PostMapping("/execute/{id}")
    public ResponseEntity<SmartContract> executeContract(@PathVariable Long id) {
        return ResponseEntity.ok(smartContractService.executeContract(id));
    }

    @PostMapping("/auto-execute")
    public ResponseEntity<Map<String, String>> autoExecute() {
        smartContractService.autoExecutePendingContracts();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Auto-execution triggered"));
    }

    @GetMapping
    public ResponseEntity<List<SmartContract>> getAllContracts() {
        return ResponseEntity.ok(smartContractService.getAllContracts());
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<Map<String, Object>> getContractStatus(@PathVariable Long id) {
        return ResponseEntity.ok(smartContractService.getContractStatus(id));
    }

    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<SmartContract>> getContractsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(smartContractService.getContractsByStatus(status));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        List<SmartContract> all = smartContractService.getAllContracts();
        long pending = all.stream().parallel().filter(c -> "PENDING".equals(c.getExecutionStatus())).count();
        long executed = all.stream().parallel().filter(c -> "EXECUTED".equals(c.getExecutionStatus())).count();
        long failed = all.stream().parallel().filter(c -> "FAILED".equals(c.getExecutionStatus())).count();

        stats.put("total", all.size());
        stats.put("pending", pending);
        stats.put("executed", executed);
        stats.put("failed", failed);
        stats.put("status", "Smart Contract System active");

        return ResponseEntity.ok(stats);
    }
}
