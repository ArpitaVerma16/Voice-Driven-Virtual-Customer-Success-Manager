package com.vcsm.service;$1

import com.vcsm.config.AppConstants;

import com.vcsm.model.Complaint;
import com.vcsm.model.Event;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@lombok.RequiredArgsConstructor
public class LiveDashboardService {

    private final ComplaintRepository complaintRepository;

    private final EventRepository eventRepository;

    // Active user tracking
    private final Map<String, Long> activeUsers = new ConcurrentHashMap<>();
    private final AtomicInteger totalActiveUsers = new AtomicInteger(0);

    // Live stats cache
    private Map<String, Object> cachedStats = new ConcurrentHashMap<>();
    private long lastUpdateTime = 0;

    public Map<String, Object> getLiveStats() {
        // Refresh cache every 5 seconds
        if (System.currentTimeMillis() - lastUpdateTime > AppConstants.CACHE_REFRESH_MS) {
            refreshStats();
        }
        return cachedStats;
    }

    private void refreshStats() {
        Map<String, Object> stats = new HashMap<>();

        // Complaint stats
        long totalComplaints = complaintRepository.count();
        long openComplaints = complaintRepository.countByStatus(Complaint.ComplaintStatus.OPEN);
        long resolvedComplaints = complaintRepository.countByStatus(Complaint.ComplaintStatus.RESOLVED);
        long inProgressComplaints = complaintRepository.countByStatus(Complaint.ComplaintStatus.IN_PROGRESS);

        stats.put("totalComplaints", totalComplaints);
        stats.put("openComplaints", openComplaints);
        stats.put("resolvedComplaints", resolvedComplaints);
        stats.put("inProgressComplaints", inProgressComplaints);

        // Event stats
        long totalEvents = eventRepository.count();
        long activeEvents = eventRepository.findByActiveTrue().size();

        stats.put("totalEvents", totalEvents);
        stats.put("activeEvents", activeEvents);

        // Resolution rate
        double resolutionRate = totalComplaints > 0 
            ? (resolvedComplaints * 100.0 / totalComplaints) 
            : 0;
        stats.put("resolutionRate", Math.round(resolutionRate));

        // Active users
        stats.put("activeUsers", totalActiveUsers.get());

        // Timestamp
        stats.put("timestamp", System.currentTimeMillis());

        cachedStats = stats;
        lastUpdateTime = System.currentTimeMillis();
    }

    public void userConnected(String sessionId, Long userId) {
        activeUsers.put(sessionId, userId);
        totalActiveUsers.set(activeUsers.size());
    }

    public void userDisconnected(String sessionId) {
        activeUsers.remove(sessionId);
        totalActiveUsers.set(activeUsers.size());
    }

    public int getActiveUserCount() {
        return totalActiveUsers.get();
    }

    public Map<String, Object> getRealtimeUpdate() {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "stats_update");
        update.put("data", getLiveStats());
        return update;
    }
}