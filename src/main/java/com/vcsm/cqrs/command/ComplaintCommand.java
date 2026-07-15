package com.vcsm.cqrs.command;

import com.vcsm.model.Complaint;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ComplaintCommand {

    private final ComplaintRepository commandRepository;

    public ComplaintCommand(ComplaintRepository commandRepository) {
        this.commandRepository = commandRepository;
    }

    public Complaint createComplaint(Complaint complaint) {
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setStatus(Complaint.ComplaintStatus.OPEN);
        return commandRepository.save(complaint);
    }

    public Complaint updateStatus(Long id, String status) {
        Complaint complaint = commandRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Complaint not found"));
        complaint.setStatus(Complaint.ComplaintStatus.valueOf(status.toUpperCase()));
        complaint.setUpdatedAt(LocalDateTime.now());
        return commandRepository.save(complaint);
    }

    public void deleteComplaint(Long id) {
        commandRepository.deleteById(id);
    }

    public Complaint updatePriority(Long id, String priority) {
        Complaint complaint = commandRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Complaint not found"));
        complaint.setPriority(priority);
        return commandRepository.save(complaint);
    }
}