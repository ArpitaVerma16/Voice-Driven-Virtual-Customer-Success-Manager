package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockchainServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private BlockchainService blockchainService;

    private Complaint complaint;

    @BeforeEach
    void setUp() {
        complaint = new Complaint();
        complaint.setId(1L);
        complaint.setResidentName("Test User");
        complaint.setDescription("Test complaint description");
        complaint.setStatus(Complaint.ComplaintStatus.OPEN);
        complaint.setCategory(Complaint.ComplaintCategory.MAINTENANCE);
        complaint.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void initBlockchain_shouldCreateGenesisBlock_whenEmpty() {
        blockchainService.initBlockchain();

        Map<String, Object> details = blockchainService.getBlockchainDetails();
        assertEquals(1, details.get("blockCount"));
        assertTrue((Boolean) details.get("isValid"));
    }

    @Test
    void initBlockchain_shouldNotCreateDuplicateGenesisBlock() {
        blockchainService.initBlockchain();
        blockchainService.initBlockchain();

        Map<String, Object> details = blockchainService.getBlockchainDetails();
        assertEquals(1, details.get("blockCount"));
    }

    @Test
    void addBlock_shouldCreateBlockWithCorrectIndex() {
        blockchainService.initBlockchain();
        BlockchainService.Block block = blockchainService.addBlock(complaint, "COMPLAINT_CREATED");

        assertNotNull(block);
        assertEquals(1, block.getIndex());
        assertEquals(complaint.getId(), block.getComplaintId());
        assertNotNull(block.getHash());
        assertNotNull(block.getTimestamp());
    }

    @Test
    void addBlock_shouldChainBlocksCorrectly() {
        blockchainService.initBlockchain();
        BlockchainService.Block block1 = blockchainService.addBlock(complaint, "COMPLAINT_CREATED");
        BlockchainService.Block block2 = blockchainService.addBlock(complaint, "STATUS_UPDATED");

        assertEquals(block1.getHash(), block2.getPreviousHash());
    }

    @Test
    void verifyComplaint_shouldReturnSuccessFalse_whenComplaintNotFound() {
        when(complaintRepository.findById(999L)).thenReturn(Optional.empty());

        Map<String, Object> result = blockchainService.verifyComplaint(999L);

        assertFalse((Boolean) result.get("success"));
        assertEquals("Complaint not found", result.get("message"));
    }

    @Test
    void verifyComplaint_shouldReturnBlockchainValidFalse_whenNoBlocksExist() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));

        Map<String, Object> result = blockchainService.verifyComplaint(1L);

        assertTrue((Boolean) result.get("success"));
        assertFalse((Boolean) result.get("blockchainValid"));
        assertEquals(0, result.get("blockCount"));
    }

    @Test
    void verifyBlockchainIntegrity_shouldReturnTrue_forValidChain() {
        blockchainService.initBlockchain();
        blockchainService.addBlock(complaint, "COMPLAINT_CREATED");
        blockchainService.addBlock(complaint, "STATUS_UPDATED");

        assertTrue(blockchainService.verifyBlockchainIntegrity());
    }

    @Test
    void getBlockchainDetails_shouldReturnCorrectCount() {
        blockchainService.initBlockchain();
        blockchainService.addBlock(complaint, "COMPLAINT_CREATED");
        blockchainService.addBlock(complaint, "STATUS_UPDATED");

        Map<String, Object> details = blockchainService.getBlockchainDetails();
        assertEquals(3, details.get("blockCount"));
        assertTrue((Boolean) details.get("isValid"));
    }

    @Test
    void generateComplaintHash_shouldReturnConsistentHash() {
        String hash1 = blockchainService.generateComplaintHash(complaint);
        String hash2 = blockchainService.generateComplaintHash(complaint);

        assertEquals(hash1, hash2);
        assertTrue(hash1.startsWith("0x"));
    }

    @Test
    void generateComplaintHash_shouldProduceDifferentHashesForDifferentComplaints() {
        Complaint other = new Complaint();
        other.setId(2L);
        other.setResidentName("Other User");
        other.setDescription("Different complaint");
        other.setStatus(Complaint.ComplaintStatus.RESOLVED);
        other.setCategory(Complaint.ComplaintCategory.NOISE);
        other.setCreatedAt(LocalDateTime.now());

        String hash1 = blockchainService.generateComplaintHash(complaint);
        String hash2 = blockchainService.generateComplaintHash(other);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void generateVerificationData_shouldReturnNull_whenComplaintNotFound() {
        when(complaintRepository.findById(999L)).thenReturn(Optional.empty());

        String data = blockchainService.generateVerificationData(999L);

        assertNull(data);
    }

    @Test
    void generateVerificationData_shouldReturnValidUrl() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));

        String data = blockchainService.generateVerificationData(1L);

        assertNotNull(data);
        assertTrue(data.startsWith("VCSM://verify/"));
        assertTrue(data.contains("hash="));
        assertTrue(data.contains("status="));
    }

    @Test
    void getComplaintBlocks_shouldFilterByComplaintId() {
        blockchainService.initBlockchain();
        blockchainService.addBlock(complaint, "COMPLAINT_CREATED");

        Complaint otherComplaint = new Complaint();
        otherComplaint.setId(2L);
        blockchainService.addBlock(otherComplaint, "COMPLAINT_CREATED");

        var blocks = blockchainService.getComplaintBlocks(1L);
        assertEquals(1, blocks.size());
        assertEquals(1L, blocks.get(0).getComplaintId());
    }

    @Test
    void getBlockchainDetails_shouldHandleEmptyBlockchain() {
        Map<String, Object> details = blockchainService.getBlockchainDetails();

        assertEquals(0, details.get("blockCount"));
        assertTrue((Boolean) details.get("isValid"));
    }
}
