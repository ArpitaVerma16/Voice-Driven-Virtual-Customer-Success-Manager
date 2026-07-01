package com.vcsm.controller;

import com.vcsm.optimization.QuantumOptimizer;
import com.vcsm.service.SchedulingOptimizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quantum")
public class QuantumController {

    @Autowired
    private SchedulingOptimizer schedulingOptimizer;

    @PostMapping("/route")
    public ResponseEntity<Map<String, Object>> optimizeRouting() {
        // Create sample data
        List<QuantumOptimizer.Complaint> complaints = new ArrayList<>();
        List<QuantumOptimizer.Admin> admins = new ArrayList<>();
        
        // Add sample complaints
        for (int i = 1; i <= 10; i++) {
            QuantumOptimizer.Complaint c = new QuantumOptimizer.Complaint();
            c.id = (long) i;
            c.description = "Complaint " + i;
            c.priority = i % 5 + 1;
            complaints.add(c);
        }
        
        // Add sample admins
        for (int i = 1; i <= 3; i++) {
            QuantumOptimizer.Admin a = new QuantumOptimizer.Admin();
            a.id = (long) i;
            a.name = "Admin " + i;
            a.expertise = "General";
            admins.add(a);
        }
        
        return ResponseEntity.ok(schedulingOptimizer.optimizeRouting(complaints, admins));
    }

    @PostMapping("/schedule")
    public ResponseEntity<Map<String, Object>> optimizeSchedule() {
        List<QuantumOptimizer.Staff> staff = new ArrayList<>();
        List<QuantumOptimizer.Shift> shifts = new ArrayList<>();
        
        // Add sample staff
        for (int i = 1; i <= 5; i++) {
            QuantumOptimizer.Staff s = new QuantumOptimizer.Staff();
            s.id = (long) i;
            s.name = "Staff " + i;
            s.skill = "General";
            staff.add(s);
        }
        
        // Add sample shifts
        for (int i = 1; i <= 7; i++) {
            QuantumOptimizer.Shift s = new QuantumOptimizer.Shift();
            s.id = (long) i;
            s.time = "Day " + i;
            shifts.add(s);
        }
        
        return ResponseEntity.ok(schedulingOptimizer.optimizeScheduling(staff, shifts));
    }

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getReport() {
        return ResponseEntity.ok(schedulingOptimizer.getOptimizationReport());
    }

    @GetMapping("/algorithms")
    public ResponseEntity<List<String>> getAlgorithms() {
        return ResponseEntity.ok(List.of(
            "Simulated Annealing",
            "Genetic Algorithm",
            "Quantum-Inspired Optimization"
        ));
    }
}