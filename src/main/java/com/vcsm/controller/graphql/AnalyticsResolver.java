package com.vcsm.controller.graphql;

import com.vcsm.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AnalyticsResolver {

    private final ComplaintService complaintService;

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AnalyticsData analytics() {
        Map<String, Long> stats = complaintService.getComplaintStats();
        List<CategoryStats> categoryDistribution = getCategoryDistribution();
        List<PriorityStats> priorityDistribution = getPriorityDistribution();
        List<MonthlyTrend> monthlyTrends = getMonthlyTrends();

        return new AnalyticsData(
            new ComplaintStats(
                stats.get("total"),
                stats.get("open"),
                stats.get("inProgress"),
                stats.get("resolved"),
                stats.get("closed")
            ),
            categoryDistribution,
            priorityDistribution,
            monthlyTrends
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CategoryStats> categoryDistribution() {
        Map<String, Long> map = complaintService.getComplaintsByCategory();
        return map.entrySet().stream()
            .map(entry -> new CategoryStats(entry.getKey(), entry.getValue()))
            .toList();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PriorityStats> priorityDistribution() {
        Map<String, Long> map = complaintService.getPriorityStats();
        return map.entrySet().stream()
            .map(entry -> new PriorityStats(entry.getKey(), entry.getValue()))
            .toList();
    }

    private List<CategoryStats> getCategoryDistribution() {
        return categoryDistribution();
    }

    private List<PriorityStats> getPriorityDistribution() {
        return priorityDistribution();
    }

    private List<MonthlyTrend> getMonthlyTrends() {
        // Simulate monthly trends - in real implementation, fetch from database
        List<MonthlyTrend> trends = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = 11; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            trends.add(new MonthlyTrend(
                month.format(formatter),
                (long) (Math.random() * 100) + 10,
                (long) (Math.random() * 80) + 5
            ));
        }
        return trends;
    }

    // DTOs
    public record AnalyticsData(
        ComplaintStats complaintStats,
        List<CategoryStats> categoryDistribution,
        List<PriorityStats> priorityDistribution,
        List<MonthlyTrend> monthlyTrends
    ) {}

    public record CategoryStats(String category, Long count) {}

    public record PriorityStats(String priority, Long count) {}

    public record MonthlyTrend(String month, Long complaints, Long resolved) {}

    public record ComplaintStats(
        long total,
        long open,
        long inProgress,
        long resolved,
        long closed
    ) {}
}