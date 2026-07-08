package com.vcsm.service;

import com.vcsm.model.SmartContract;
import com.vcsm.repository.SmartContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@lombok.RequiredArgsConstructor
public class SmartContractService {
    private static final Logger log = LoggerFactory.getLogger(SmartContractService.class);

    private final SmartContractRepository smartContractRepository;

    private final OracleService oracleService;

    /**
     * Create a new smart contract
     */
    public SmartContract createContract(SmartContract contract) {
        // Generate unique contract address
        contract.setContractAddress(generateContractAddress());
        contract.setExecutionStatus("PENDING");
        contract.setCreatedAt(LocalDateTime.now());
        
        return smartContractRepository.save(contract);
    }

    /**
     * Execute a smart contract
     */
    public SmartContract executeContract(Long contractId) {
        SmartContract contract = smartContractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Verify condition
        boolean conditionMet = oracleService.verifyCondition(contract);

        if (conditionMet) {
            contract.setExecutionStatus("EXECUTED");
            contract.setExecutedAt(LocalDateTime.now());
            contract.setTransactionHash(generateTransactionHash());
            
            // Execute based on contract type
            executeContractLogic(contract);
        } else {
            contract.setExecutionStatus("FAILED");
        }

        return smartContractRepository.save(contract);
    }

    /**
     * Auto-execute pending contracts
     */
    public void autoExecutePendingContracts() {
        List<SmartContract> pendingContracts = smartContractRepository
            .findByAutoExecuteTrueAndExecutionStatus("PENDING");

        for (SmartContract contract : pendingContracts) {
            boolean conditionMet = oracleService.verifyCondition(contract);
            if (conditionMet) {
                executeContract(contract.getId());
            }
        }
    }

    private void executeContractLogic(SmartContract contract) {
        switch (contract.getConditionType()) {
            case "RESOLUTION":
                log.info("✅ Auto-resolving complaint: " + contract.getComplaintId());
                break;
            case "PAYMENT":
                log.info("💰 Processing payment of " + contract.getAmount() + " for contract: " + contract.getId());
                break;
            case "COMPLETION":
                log.info("🎉 Event completed: " + contract.getEventId());
                break;
        }
    }

    private String generateContractAddress() {
        return "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 40);
    }

    private String generateTransactionHash() {
        return "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 64);
    }

    /**
     * Get contract status
     */
    public Map<String, Object> getContractStatus(Long contractId) {
        Optional<SmartContract> contractOpt = smartContractRepository.findById(contractId);
        if (contractOpt.isEmpty()) {
            return Map.of("error", "Contract not found");
        }

        SmartContract contract = contractOpt.get();
        Map<String, Object> status = new HashMap<>();
        status.put("contractId", contract.getId());
        status.put("contractName", contract.getContractName());
        status.put("status", contract.getExecutionStatus());
        status.put("contractAddress", contract.getContractAddress());
        status.put("createdAt", contract.getCreatedAt());
        status.put("executedAt", contract.getExecutedAt());
        status.put("transactionHash", contract.getTransactionHash());

        return status;
    }

    /**
     * Get all contracts
     */
    public List<SmartContract> getAllContracts() {
        return smartContractRepository.findAll();
    }

    /**
     * Get contracts by status
     */
    public List<SmartContract> getContractsByStatus(String status) {
        return smartContractRepository.findByExecutionStatus(status);
    }
}