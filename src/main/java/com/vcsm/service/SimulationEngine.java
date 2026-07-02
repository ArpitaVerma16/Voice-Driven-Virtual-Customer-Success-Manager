package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.DigitalTwin;
import com.vcsm.model.Event;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Profile("dev")
@Service
public class SimulationEngine {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private EventRepository eventRepository;

    /**
     * Run simulation on digital twin
     */
    public SimulationResult runSimulation(DigitalTwin twin, SimulationScenario scenario) {
        // Create copy of production data
        List<Complaint> prodComplaints = complaintRepository.findAll();
        List<Event> prodEvents = eventRepository.findAll();

        // Apply scenario to twin data
        List<Complaint> twinComplaints = applyScenarioToComplaints(prodComplaints, scenario);
        List<Event> twinEvents = applyScenarioToEvents(prodEvents, scenario);

        // Run simulation
        long startTime = System.currentTimeMillis();
        SimulationResult result = simulate(twinComplaints, twinEvents, scenario);
        long endTime = System.currentTimeMillis();

        result.setSimulationTime(endTime - startTime);
        result.setTwinId(twin.getId());
        result.setTwinName(twin.getTwinName());
        result.setScenarioName(scenario.getName());

        return result;
    }

    private List<Complaint> applyScenarioToComplaints(List<Complaint> complaints, SimulationScenario scenario) {
        List<Complaint> simulated = complaints.stream()
            .map(c -> copyComplaint(c))
            .collect(Collectors.toList());

        switch (scenario.getType()) {
            case "LOAD_TEST":
                // Multiply complaints
                for (int i = 0; i < scenario.getMultiplier(); i++) {
                    for (Complaint c : complaints) {
                        Complaint copy = copyComplaint(c);
                        copy.setId(null);
                        copy.setDescription(c.getDescription() + " (Simulated)");
                        simulated.add(copy);
                    }
                }
                break;
            case "FAILURE_TEST":
                // Random failures
                for (Complaint c : simulated) {
                    if (new Random().nextDouble() < 0.2) {
                        c.setStatus(Complaint.ComplaintStatus.CLOSED);
                        c.setResolutionNotes("Failed due to simulation error");
                    }
                }
                break;
            case "CAPACITY_TEST":
                // Stress test
                for (Complaint c : simulated) {
                    c.setPriority("HIGH");
                }
                break;
            default:
                break;
        }

        return simulated;
    }

    private List<Event> applyScenarioToEvents(List<Event> events, SimulationScenario scenario) {
        List<Event> simulated = events.stream()
            .map(e -> copyEvent(e))
            .collect(Collectors.toList());

        if ("LOAD_TEST".equals(scenario.getType())) {
            for (Event e : events) {
                Event copy = copyEvent(e);
                copy.setId(null);
                copy.setName(e.getName() + " (Simulated)");
                simulated.add(copy);
            }
        }

        return simulated;
    }

    private Complaint copyComplaint(Complaint c) {
        Complaint copy = new Complaint();
        copy.setId(c.getId());
        copy.setResidentName(c.getResidentName());
        copy.setDescription(c.getDescription());
        copy.setCategory(c.getCategory());
        copy.setStatus(c.getStatus());
        copy.setPriority(c.getPriority());
        copy.setCreatedAt(c.getCreatedAt());
        return copy;
    }

    private Event copyEvent(Event e) {
        Event copy = new Event();
        copy.setId(e.getId());
        copy.setName(e.getName());
        copy.setDescription(e.getDescription());
        copy.setCategory(e.getCategory());
        copy.setEventDate(e.getEventDate());
        copy.setRegistrations(e.getRegistrations());
        copy.setMaxCapacity(e.getMaxCapacity());
        return copy;
    }

    private SimulationResult simulate(List<Complaint> complaints, List<Event> events, SimulationScenario scenario) {
        SimulationResult result = new SimulationResult();

        // Calculate metrics
        long totalComplaints = complaints.size();
        long resolvedComplaints = complaints.stream()
            .filter(c -> c.getStatus() == Complaint.ComplaintStatus.RESOLVED)
            .count();

        long totalEvents = events.size();
        long activeEvents = events.stream().filter(Event::isActive).count();

        result.setTotalComplaints(totalComplaints);
        result.setResolvedComplaints(resolvedComplaints);
        result.setTotalEvents(totalEvents);
        result.setActiveEvents(activeEvents);

        // Performance metrics
        double resolutionRate = totalComplaints > 0 ? (resolvedComplaints * 100.0 / totalComplaints) : 0;
        result.setResolutionRate(Math.round(resolutionRate));

        // Impact assessment
        double impact = scenario.getType().equals("LOAD_TEST") ? 0.25 : 0.1;
        result.setPerformanceImpact(impact);
        result.setSystemStable(impact < 0.5);

        return result;
    }

    public static class SimulationScenario {
        private String name;
        private String type; // LOAD_TEST, FAILURE_TEST, CAPACITY_TEST
        private int multiplier = 1;

        public SimulationScenario(String name, String type, int multiplier) {
            this.name = name;
            this.type = type;
            this.multiplier = multiplier;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public int getMultiplier() { return multiplier; }
    }

    public static class SimulationResult {
        private Long twinId;
        private String twinName;
        private String scenarioName;
        private long totalComplaints;
        private long resolvedComplaints;
        private long totalEvents;
        private long activeEvents;
        private double resolutionRate;
        private double performanceImpact;
        private boolean systemStable;
        private long simulationTime;

        // Getters and Setters
        public Long getTwinId() { return twinId; }
        public void setTwinId(Long twinId) { this.twinId = twinId; }
        public String getTwinName() { return twinName; }
        public void setTwinName(String twinName) { this.twinName = twinName; }
        public String getScenarioName() { return scenarioName; }
        public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
        public long getTotalComplaints() { return totalComplaints; }
        public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }
        public long getResolvedComplaints() { return resolvedComplaints; }
        public void setResolvedComplaints(long resolvedComplaints) { this.resolvedComplaints = resolvedComplaints; }
        public long getTotalEvents() { return totalEvents; }
        public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }
        public long getActiveEvents() { return activeEvents; }
        public void setActiveEvents(long activeEvents) { this.activeEvents = activeEvents; }
        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
        public double getPerformanceImpact() { return performanceImpact; }
        public void setPerformanceImpact(double performanceImpact) { this.performanceImpact = performanceImpact; }
        public boolean isSystemStable() { return systemStable; }
        public void setSystemStable(boolean systemStable) { this.systemStable = systemStable; }
        public long getSimulationTime() { return simulationTime; }
        public void setSimulationTime(long simulationTime) { this.simulationTime = simulationTime; }
    }
}
