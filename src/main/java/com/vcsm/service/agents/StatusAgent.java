package com.vcsm.service.agents;

import com.vcsm.model.Complaint;
import com.vcsm.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@lombok.RequiredArgsConstructor
public class StatusAgent {

    private final ComplaintService complaintService;

    public Map<String, Object> process(String query, Long userId) {
        Map<String, Object> response = new HashMap<>();

        List<Complaint> complaints = complaintService.getAllComplaints();

        if (complaints.isEmpty()) {
            response.put("success", true);
            response.put("action", "status");
            response.put("message", "You have no complaints.");
            return response;
        }

        long open = complaints.stream()
            .filter(c -> c.getStatus() == Complaint.ComplaintStatus.OPEN)
            .count();
        long inProgress = complaints.stream()
            .filter(c -> c.getStatus() == Complaint.ComplaintStatus.IN_PROGRESS)
            .count();
        long resolved = complaints.stream()
            .filter(c -> c.getStatus() == Complaint.ComplaintStatus.RESOLVED)
            .count();

        response.put("success", true);
        response.put("action", "status");
        response.put("message", "Here's your complaint status:");
        response.put("open", open);
        response.put("inProgress", inProgress);
        response.put("resolved", resolved);
        response.put("total", complaints.size());

        return response;
    }
}