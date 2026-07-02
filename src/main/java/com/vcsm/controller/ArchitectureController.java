package com.vcsm.controller;

import com.vcsm.enas.EvolutionaryEngine;
import com.vcsm.enas.NeuralArchitecture;
import com.vcsm.enas.PerformancePredictor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enas")
@CrossOrigin(origins = "*")
public class ArchitectureController {

    @Autowired
    private EvolutionaryEngine evolutionaryEngine;

    @Autowired
    private PerformancePredictor performancePredictor;

    @PostMapping("/evolve")
    public ResponseEntity<EvolutionaryEngine.EvolutionResult> evolve() {
        return ResponseEntity.ok(evolutionaryEngine.runEvolution());
    }

    @PostMapping("/predict")
    public ResponseEntity<PerformancePredictor.PredictedPerformance> predict(@Valid @RequestBody NeuralArchitecture architecture) {
        return ResponseEntity.ok(performancePredictor.predict(architecture));
    }

    @PostMapping("/compare")
    public ResponseEntity<PerformancePredictor.ComparisonResult> compare(@Valid @RequestBody CompareRequest request) {
        return ResponseEntity.ok(performancePredictor.compare(request.getArch1(), request.getArch2()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("status", "Evolutionary Neural Architecture Search active");
        stats.put("populationSize", EvolutionaryEngine.POPULATION_SIZE);
        stats.put("generations", EvolutionaryEngine.GENERATIONS);
        stats.put("mutationRate", EvolutionaryEngine.MUTATION_RATE);
        stats.put("crossoverRate", EvolutionaryEngine.CROSSOVER_RATE);
        stats.put("eliteCount", EvolutionaryEngine.ELITE_COUNT);
        return ResponseEntity.ok(stats);
    }

    public static class CompareRequest {
        private NeuralArchitecture arch1;
        private NeuralArchitecture arch2;

        public NeuralArchitecture getArch1() { return arch1; }
        public void setArch1(NeuralArchitecture arch1) { this.arch1 = arch1; }
        public NeuralArchitecture getArch2() { return arch2; }
        public void setArch2(NeuralArchitecture arch2) { this.arch2 = arch2; }
    }
}
