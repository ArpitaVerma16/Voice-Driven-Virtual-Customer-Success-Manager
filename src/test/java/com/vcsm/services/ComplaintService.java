package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.repository.ComplaintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ComplaintService {
    
    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);
    
    @Autowired
    private ComplaintRepository complaintRepository;
    
    public Complaint fileComplaint(Complaint complaint) {
        log.info("📝 Filing new complaint by: {}", complaint.getResidentName());
        log.debug("Complaint details - Category: {}, Description: {}", 
                  complaint.getCategory(), complaint.getDescription());
        
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setStatus(Complaint.ComplaintStatus.OPEN);
        
        Complaint saved = complaintRepository.save(complaint);
        
        log.info("✅ Complaint filed successfully with ID: {}", saved.getId());
        return saved;
    }
    
    public List<Complaint> getAllComplaints() {
        log.debug("Fetching all complaints");
        return complaintRepository.findAll();
    }
    
    public Optional<Complaint> getComplaintById(Long id) {
        log.debug("Fetching complaint with ID: {}", id);
        return complaintRepository.findById(id);
    }
    
    public Complaint updateComplaintStatus(Long id, Complaint.ComplaintStatus status) {
        log.info("🔄 Updating complaint {} status to: {}", id, status);
        
        Complaint complaint = complaintRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Complaint not found"));
        
        complaint.setStatus(status);
        if (status == Complaint.ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }
        
        Complaint updated = complaintRepository.save(complaint);
        log.info("✅ Complaint {} status updated to: {}", id, status);
        return updated;
    }
    
    public void deleteComplaint(Long id) {
        log.warn("⚠️ Deleting complaint with ID: {}", id);
        complaintRepository.deleteById(id);
        log.info("🗑️ Complaint {} deleted successfully", id);
    }
}