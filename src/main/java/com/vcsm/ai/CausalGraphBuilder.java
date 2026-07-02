package com.vcsm.ai;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CausalGraphBuilder {

    private final Map<String, List<String>> graph = new HashMap<>();

    public CausalGraphBuilder() {
        buildCausalGraph();
    }

    private void buildCausalGraph() {
        // Complaint causes
        graph.put("NOISE_COMPLAINT", Arrays.asList("NOISE_SOURCE", "RESIDENT_FRUSTRATION", "COMPLAINT_FILED"));
        graph.put("NOISE_SOURCE", Arrays.asList("CONSTRUCTION", "NEIGHBOR_ACTIVITY", "ANIMALS"));
        
        graph.put("MAINTENANCE_COMPLAINT", Arrays.asList("EQUIPMENT_FAILURE", "AGING_INFRASTRUCTURE", "POOR_MAINTENANCE"));
        graph.put("EQUIPMENT_FAILURE", Arrays.asList("LACK_SERVICING", "OLD_AGE", "MANUFACTURING_DEFECT"));
        
        graph.put("SECURITY_COMPLAINT", Arrays.asList("SECURITY_BREACH", "INADEQUATE_SURVEILLANCE", "STAFF_NEGLIGENCE"));
        graph.put("SECURITY_BREACH", Arrays.asList("UNAUTHORIZED_ENTRY", "POOR_LIGHTING", "GATE_MALFUNCTION"));

        // Resolution causes
        graph.put("RESOLUTION_FAILURE", Arrays.asList("INSUFFICIENT_STAFF", "LACK_EXPERTISE", "POOR_COMMUNICATION"));
        graph.put("DELAYED_RESOLUTION", Arrays.asList("HIGH_VOLUME", "COMPLEX_ISSUE", "WAITING_PARTS"));

        // Event causes
        graph.put("EVENT_SUCCESS", Arrays.asList("GOOD_PROMOTION", "INTERESTING_TOPIC", "CONVENIENT_TIMING"));
        graph.put("EVENT_FAILURE", Arrays.asList("POOR_PROMOTION", "BAD_TIMING", "UNINTERESTING_TOPIC"));
    }

    public List<String> getCauses(String effect) {
        return graph.getOrDefault(effect, new ArrayList<>());
    }

    public List<String> getEffects(String cause) {
        List<String> effects = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : graph.entrySet()) {
            if (entry.getValue().contains(cause)) {
                effects.add(entry.getKey());
            }
        }
        return effects;
    }

    public Map<String, List<String>> getGraph() {
        return graph;
    }

    public String getPath(String start, String end) {
        // Simple BFS path finding
        Queue<String> queue = new LinkedList<>();
        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(end)) {
                return buildPath(parent, start, end);
            }

            List<String> neighbors = graph.get(current);
            if (neighbors != null) {
                for (String neighbor : neighbors) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        parent.put(neighbor, current);
                        queue.add(neighbor);
                    }
                }
            }
        }

        return org.springframework.http.ResponseEntity.ok("No path found");
    }

    private String buildPath(Map<String, String> parent, String start, String end) {
        List<String> path = new ArrayList<>();
        String current = end;
        while (!current.equals(start)) {
            path.add(0, current);
            current = parent.get(current);
        }
        path.add(0, start);
        return String.join(" → ", path);
    }
}