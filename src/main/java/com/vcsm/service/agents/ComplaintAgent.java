package com.vcsm.service.agents;

import com.vcsm.model.Complaint;
import com.vcsm.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@lombok.RequiredArgsConstructor
public class ComplaintAgent {

    private final ComplaintService complaintService;

    public Map<String, Object> process(String query, Long userId) {
        Map<String, Object> response = new HashMap<>();

        // Check if it's a complaint filing request
        if (query.toLowerCase().contains("file") || 
            query.toLowerCase().contains("register") || 
            query.toLowerCase().contains("new")) {
            
            // Extract complaint details (simplified)
            Complaint complaint = new Complaint();
            complaint.setResidentName("User " + userId);
            complaint.setDescription(query);
            complaint.setCategory(extractCategory(query));
            
            Complaint saved = complaintService.fileComplaint(complaint);
            
            response.put("success", true);
            response.put("action", "complaint_filed");
            response.put("message", "Complaint filed successfully! ID: " + saved.getId());
            response.put("complaintId", saved.getId());
        } else {
            // Query existing complaints
            response.put("success", true);
            response.put("action", "complaint_query");
            response.put("message", "I can help you file a complaint. Just tell me what the issue is.");
        }

        return response;
    }

    private Complaint.ComplaintCategory extractCategory(String query) {
    String lower = query.toLowerCase();

    if (lower.contains("noise"))
        return Complaint.ComplaintCategory.NOISE;

    if (lower.contains("maintenance") || lower.contains("repair"))
        return Complaint.ComplaintCategory.MAINTENANCE;

    if (lower.contains("security") || lower.contains("safety"))
        return Complaint.ComplaintCategory.SECURITY;

    if (lower.contains("cleanliness") || lower.contains("garbage"))
        return Complaint.ComplaintCategory.CLEANLINESS;

    if (lower.contains("parking"))
        return Complaint.ComplaintCategory.PARKING;

    if (lower.contains("utilities") || lower.contains("water") || lower.contains("electricity"))
        return Complaint.ComplaintCategory.UTILITIES;

    return Complaint.ComplaintCategory.OTHER;
}
}
