package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.repository.ComplaintRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BlockchainService {

    private final ComplaintRepository complaintRepository;
    private final Web3j web3j;

    // In-memory blockchain (simulated)
    private final List<Block> blockchain = new ArrayList<>();
    private static final String GENESIS_HASH = "0x0000000000000000000000000000000000000000";

    public BlockchainService(ComplaintRepository complaintRepository, Web3j web3j) {
        this.complaintRepository = complaintRepository;
        this.web3j = web3j;
    }

    /**
     * Initialize blockchain with genesis block
     */
    @PostConstruct
    public void initBlockchain() {
        if (blockchain.isEmpty()) {
            Block genesisBlock = new Block(
                0,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "Genesis Block",
                GENESIS_HASH,
                null
            );
            genesisBlock.setHash(calculateHash(genesisBlock));
            blockchain.add(genesisBlock);
        }
    }

    /**
     * Create a new block for a complaint
     */
    public Block createBlock(Complaint complaint, String action, String previousHash) {
        String data = String.format(
            "Complaint #%d | Action: %s | Status: %s | Category: %s | Timestamp: %s",
            complaint.getId(),
            action,
            complaint.getStatus(),
            complaint.getCategory(),
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        Block block = new Block(
            blockchain.size(),
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            data,
            previousHash != null ? previousHash : getLatestBlockHash(),
            complaint.getId()
        );
        block.setHash(calculateHash(block));

        return block;
    }

    /**
     * Add a block to the blockchain
     */
    public Block addBlock(Complaint complaint, String action) {
        String previousHash = getLatestBlockHash();
        Block block = createBlock(complaint, action, previousHash);
        blockchain.add(block);
        return block;
    }

    /**
     * Generate blockchain hash for a complaint
     */
    public String generateComplaintHash(Complaint complaint) {
        String data = String.format(
            "%d|%s|%s|%s|%s|%s",
            complaint.getId(),
            complaint.getResidentName(),
            complaint.getDescription(),
            complaint.getStatus(),
            complaint.getCategory(),
            complaint.getCreatedAt()
        );
        return sha256(data);
    }

    /**
     * Verify a complaint using blockchain
     */
    public Map<String, Object> verifyComplaint(Long complaintId) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Complaint not found");
            return result;
        }

        Complaint complaint = complaintOpt.get();
        
        // Find blocks for this complaint
        List<Block> complaintBlocks = blockchain.stream()
            .filter(b -> b.getComplaintId() != null && b.getComplaintId().equals(complaintId))
            .toList();

        result.put("success", true);
        result.put("complaintId", complaintId);
        result.put("complaintStatus", complaint.getStatus());
        result.put("blockCount", complaintBlocks.size());
        result.put("hash", generateComplaintHash(complaint));
        result.put("verificationLink", "/verify/" + complaintId);
        
        if (!complaintBlocks.isEmpty()) {
            result.put("blockchainHistory", complaintBlocks);
            result.put("blockchainValid", verifyBlockchainIntegrity());
        } else {
            result.put("blockchainHistory", new ArrayList<>());
            result.put("blockchainValid", false);
            result.put("message", "No blockchain records found for this complaint");
        }

        return result;
    }

    /**
     * Verify blockchain integrity
     */
    public boolean verifyBlockchainIntegrity() {
        for (int i = 1; i < blockchain.size(); i++) {
            Block current = blockchain.get(i);
            Block previous = blockchain.get(i - 1);
            
            if (!current.getPreviousHash().equals(previous.getHash())) {
                return false;
            }
            
            String calculatedHash = calculateHash(current);
            if (!calculatedHash.equals(current.getHash())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get blockchain details
     */
    public Map<String, Object> getBlockchainDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("blockCount", blockchain.size());
        details.put("isValid", verifyBlockchainIntegrity());
        details.put("latestBlock", blockchain.isEmpty() ? null : blockchain.get(blockchain.size() - 1));
        details.put("blocks", blockchain);
        return details;
    }

    /**
     * Get complaint block history
     */
    public List<Block> getComplaintBlocks(Long complaintId) {
        return blockchain.stream()
            .filter(b -> b.getComplaintId() != null && b.getComplaintId().equals(complaintId))
            .toList();
    }

    /**
     * Generate QR code data for verification
     */
    public String generateVerificationData(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint == null) {
            return null;
        }
        
        String hash = generateComplaintHash(complaint);
        return String.format(
            "VCSM://verify/%d?hash=%s&status=%s&timestamp=%s",
            complaintId,
            hash,
            complaint.getStatus(),
            complaint.getCreatedAt()
        );
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private String getLatestBlockHash() {
        if (blockchain.isEmpty()) {
            return GENESIS_HASH;
        }
        return blockchain.get(blockchain.size() - 1).getHash();
    }

    private String calculateHash(Block block) {
        String data = block.getIndex() + block.getTimestamp() + block.getData() + block.getPreviousHash();
        if (block.getComplaintId() != null) {
            data += block.getComplaintId();
        }
        return sha256(data);
    }

    private String sha256(String input) {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] hash = Hash.sha3(inputBytes);
        StringBuffer hexString = new StringBuffer();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return "0x" + hexString.toString();
    }

    // ============================================================
    // Block Class
    // ============================================================

    public static class Block {
        private final int index;
        private final String timestamp;
        private final String data;
        private final String previousHash;
        private final Long complaintId;
        private String hash;

        public Block(int index, String timestamp, String data, String previousHash, Long complaintId) {
            this.index = index;
            this.timestamp = timestamp;
            this.data = data;
            this.previousHash = previousHash;
            this.complaintId = complaintId;
        }

        public int getIndex() { return index; }
        public String getTimestamp() { return timestamp; }
        public String getData() { return data; }
        public String getPreviousHash() { return previousHash; }
        public Long getComplaintId() { return complaintId; }
        public String getHash() { return hash; }
        public void setHash(String hash) { this.hash = hash; }
    }
}