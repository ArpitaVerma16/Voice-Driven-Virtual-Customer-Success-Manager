package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.SmartContract;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@lombok.RequiredArgsConstructor
public class OracleService {

    private final ComplaintRepository complaintRepository;

    private final EventRepository eventRepository;

    /**
     * Fetch data from external sources (oracle)
     */
    public Map<String, Object> fetchData(String dataType, Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("dataType", dataType);

        switch (dataType) {
            case "COMPLAINT_STATUS":
                complaintRepository.findById(id).ifPresent(c -> {
                    data.put("status", c.getStatus());
                    data.put("resolvedAt", c.getUpdatedAt());
                });
                break;
            case "EVENT_ATTENDANCE":
                eventRepository.findById(id).ifPresent(e -> {
                    data.put("registrations", e.getRegistrations());
                    data.put("capacity", e.getMaxCapacity());
                });
                break;
            default:
                data.put("value", "Unknown");
        }

        return data;
    }

    /**
     * Verify condition for smart contract execution
     */
    public boolean verifyCondition(SmartContract contract) {
        Map<String, Object> data = fetchData(contract.getConditionType(), 
            contract.getComplaintId() != null ? contract.getComplaintId() : contract.getEventId());

        switch (contract.getConditionType()) {
            case "RESOLUTION":
                return "RESOLVED".equals(data.get("status")) || "CLOSED".equals(data.get("status"));
            case "COMPLETION":
                Integer registrations = (Integer) data.get("registrations");
                Integer capacity = (Integer) data.get("capacity");
                return registrations != null && capacity != null && registrations >= capacity * 0.5;
            case "PAYMENT":
                return true;
            default:
                return false;
        }
    }
}