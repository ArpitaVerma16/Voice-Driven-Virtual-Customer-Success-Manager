package com.vcsm.cqrs.query;

import com.vcsm.model.Complaint;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ComplaintQuery {

    private final ComplaintReadRepository readRepository;

    public ComplaintQuery(ComplaintReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    public ComplaintReadModel getComplaintById(Long id) {
        return readRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    public List<ComplaintReadModel> getAllComplaints() {
        return readRepository.findAll();
    }

    public List<ComplaintReadModel> getComplaintsByStatus(String status) {
        return readRepository.findByStatus(status);
    }

    public List<ComplaintReadModel> getComplaintsByCategory(String category) {
        return readRepository.findByCategory(category);
    }

    public List<ComplaintReadModel> getComplaintsByPriority(String priority) {
        return readRepository.findByPriority(priority);
    }

    public List<ComplaintReadModel> getComplaintsByDateRange(LocalDateTime start, LocalDateTime end) {
        return readRepository.findByCreatedAtBetween(start, end);
    }

    public ComplaintAnalytics getAnalytics() {
        List<ComplaintReadModel> all = readRepository.findAll();
        List<ComplaintReadModel> open = readRepository.findByStatus("OPEN");
        List<ComplaintReadModel> resolved = readRepository.findByStatus("RESOLVED");
        List<ComplaintReadModel> inProgress = readRepository.findByStatus("IN_PROGRESS");

        // Category breakdown
        Map<String, Long> categoryBreakdown = all.stream()
            .collect(Collectors.groupingBy(
                ComplaintReadModel::getCategory,
                Collectors.counting()
            ));

        // Priority breakdown
        Map<String, Long> priorityBreakdown = all.stream()
            .collect(Collectors.groupingBy(
                ComplaintReadModel::getPriority,
                Collectors.counting()
            ));

        return new ComplaintAnalytics(
            all.size(),
            open.size(),
            resolved.size(),
            inProgress.size(),
            categoryBreakdown,
            priorityBreakdown,
            all.stream().mapToDouble(ComplaintReadModel::getAvgResolutionTime).average().orElse(0.0)
        );
    }

    public static class ComplaintAnalytics {
        private final long total;
        private final long open;
        private final long resolved;
        private final long inProgress;
        private final Map<String, Long> categoryBreakdown;
        private final Map<String, Long> priorityBreakdown;
        private final double avgResolutionTime;

        public ComplaintAnalytics(long total, long open, long resolved, long inProgress,
                                  Map<String, Long> categoryBreakdown, Map<String, Long> priorityBreakdown,
                                  double avgResolutionTime) {
            this.total = total;
            this.open = open;
            this.resolved = resolved;
            this.inProgress = inProgress;
            this.categoryBreakdown = categoryBreakdown;
            this.priorityBreakdown = priorityBreakdown;
            this.avgResolutionTime = avgResolutionTime;
        }

        public long getTotal() { return total; }
        public long getOpen() { return open; }
        public long getResolved() { return resolved; }
        public long getInProgress() { return inProgress; }
        public Map<String, Long> getCategoryBreakdown() { return categoryBreakdown; }
        public Map<String, Long> getPriorityBreakdown() { return priorityBreakdown; }
        public double getAvgResolutionTime() { return avgResolutionTime; }
    }
}