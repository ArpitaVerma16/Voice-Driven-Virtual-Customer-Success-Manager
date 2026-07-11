package com.vcsm.optimization;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class QuantumOptimizer {

    /**
     * Simulated Annealing for complaint routing optimization
     */
    public RoutingSolution optimizeRouting(List<Complaint> complaints, List<Admin> admins) {
        // Initial random assignment
        RoutingSolution current = new RoutingSolution(complaints, admins);
        current.randomAssign();

        RoutingSolution best = current.copy();
        double temperature = 100.0;
        double coolingRate = 0.995;
        int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            RoutingSolution neighbor = current.copy();
            neighbor.swapRandomAssignment();

            double currentCost = current.getTotalCost();
            double neighborCost = neighbor.getTotalCost();

            if (neighborCost < currentCost || Math.exp(-(neighborCost - currentCost) / temperature) > Math.random()) {
                current = neighbor;
            }

            if (current.getTotalCost() < best.getTotalCost()) {
                best = current.copy();
            }

            temperature *= coolingRate;
        }

        return best;
    }

    /**
     * Genetic Algorithm for staff scheduling
     */
    public StaffSchedule optimizeSchedule(List<Staff> staff, List<Shift> shifts) {
        int populationSize = 100;
        int generations = 1000;
        double mutationRate = 0.1;
        double crossoverRate = 0.8;

        List<StaffSchedule> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(new StaffSchedule(staff, shifts));
        }

        for (int gen = 0; gen < generations; gen++) {
            // Evaluate fitness
            for (StaffSchedule schedule : population) {
                schedule.calculateFitness();
            }

            // Sort by fitness (best first)
            population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));

            // Elitism - keep top 10%
            List<StaffSchedule> newPopulation = new ArrayList<>();
            for (int i = 0; i < populationSize * 0.1; i++) {
                newPopulation.add(population.get(i));
            }

            // Crossover and mutation
            while (newPopulation.size() < populationSize) {
                StaffSchedule parent1 = tournamentSelect(population);
                StaffSchedule parent2 = tournamentSelect(population);

                StaffSchedule child;
                if (Math.random() < crossoverRate) {
                    child = crossover(parent1, parent2);
                } else {
                    child = parent1.copy();
                }

                if (Math.random() < mutationRate) {
                    child.mutate();
                }

                newPopulation.add(child);
            }

            population = newPopulation;
        }

        // Return best schedule
        return population.stream()
            .max(Comparator.comparingDouble(StaffSchedule::getFitness))
            .orElse(null);
    }

    private StaffSchedule tournamentSelect(List<StaffSchedule> population) {
        int tournamentSize = 5;
        StaffSchedule best = population.get(ThreadLocalRandom.current().nextInt(population.size()));
        for (int i = 1; i < tournamentSize; i++) {
            StaffSchedule candidate = population.get(ThreadLocalRandom.current().nextInt(population.size()));
            if (candidate.getFitness() > best.getFitness()) {
                best = candidate;
            }
        }
        return best;
    }

    private StaffSchedule crossover(StaffSchedule parent1, StaffSchedule parent2) {
        StaffSchedule child = parent1.copy();
        int crossoverPoint = ThreadLocalRandom.current().nextInt(child.shifts.size());
        for (int i = crossoverPoint; i < child.shifts.size(); i++) {
            child.shifts.set(i, parent2.shifts.get(i));
        }
        return child;
    }

    /**
     * Resource allocation optimization
     */
    public ResourceAllocation optimizeResources(List<Resource> resources, List<Task> tasks) {
        ResourceAllocation allocation = new ResourceAllocation(resources, tasks);
        
        // Greedy initial allocation
        for (Task task : tasks) {
            Resource bestResource = null;
            double bestScore = Double.MAX_VALUE;

            for (Resource resource : resources) {
                double score = resource.getCost(task) - resource.getCapacity();
                if (score < bestScore && resource.getCapacity() >= task.getRequirement()) {
                    bestScore = score;
                    bestResource = resource;
                }
            }

            if (bestResource != null) {
                allocation.assign(bestResource, task);
                bestResource.reduceCapacity(task.getRequirement());
            }
        }

        return allocation;
    }

    // Inner classes
    public static class RoutingSolution {
        private final List<Complaint> complaints;
        private final List<Admin> admins;
        private final Map<Complaint, Admin> assignment = new HashMap<>();

        public RoutingSolution(List<Complaint> complaints, List<Admin> admins) {
            this.complaints = complaints;
            this.admins = admins;
        }

        public void randomAssign() {
            for (Complaint complaint : complaints) {
                Admin randomAdmin = admins.get(ThreadLocalRandom.current().nextInt(admins.size()));
                assignment.put(complaint, randomAdmin);
            }
        }

        public void swapRandomAssignment() {
            if (complaints.size() < 2) return;
            int idx1 = ThreadLocalRandom.current().nextInt(complaints.size());
            int idx2 = ThreadLocalRandom.current().nextInt(complaints.size());
            Complaint c1 = complaints.get(idx1);
            Complaint c2 = complaints.get(idx2);
            Admin temp = assignment.get(c1);
            assignment.put(c1, assignment.get(c2));
            assignment.put(c2, temp);
        }

        public double getTotalCost() {
            double cost = 0;
            for (Map.Entry<Complaint, Admin> entry : assignment.entrySet()) {
                cost += entry.getValue().getWorkload();
            }
            return cost;
        }

        public RoutingSolution copy() {
            RoutingSolution copy = new RoutingSolution(complaints, admins);
            copy.assignment.putAll(this.assignment);
            return copy;
        }
    }

    public static class StaffSchedule {
        private final List<Staff> staff;
        private final List<Shift> shifts;
        private final List<Shift> schedule = new ArrayList<>();
        private double fitness = 0;

        public StaffSchedule(List<Staff> staff, List<Shift> shifts) {
            this.staff = staff;
            this.shifts = shifts;
            randomGenerate();
        }

        private void randomGenerate() {
            for (Shift shift : shifts) {
                Staff randomStaff = staff.get(ThreadLocalRandom.current().nextInt(staff.size()));
                schedule.add(shift);
            }
        }

        public void calculateFitness() {
            // Fitness based on coverage and fairness
            Map<Staff, Integer> shiftCount = new HashMap<>();
            for (Staff s : staff) shiftCount.put(s, 0);
            for (Shift shift : schedule) {
                Staff assigned = shift.getAssignedStaff();
                if (assigned != null) {
                    shiftCount.put(assigned, shiftCount.getOrDefault(assigned, 0) + 1);
                }
            }

            // Maximize coverage, minimize variance
            double coverage = schedule.stream().parallel().filter(s -> s.getAssignedStaff() != null).count();
            double variance = calculateVariance(shiftCount);

            this.fitness = coverage * 0.7 + (100 - variance) * 0.3;
        }

        private double calculateVariance(Map<Staff, Integer> counts) {
            double avg = counts.values().stream().mapToInt(Integer::intValue).average().orElse(0);
            return counts.values().stream()
                .mapToDouble(c -> Math.pow(c - avg, 2))
                .average().orElse(0);
        }

        public void mutate() {
            int idx = ThreadLocalRandom.current().nextInt(schedule.size());
            Staff randomStaff = staff.get(ThreadLocalRandom.current().nextInt(staff.size()));
            schedule.get(idx).setAssignedStaff(randomStaff);
        }

        public StaffSchedule copy() {
            StaffSchedule copy = new StaffSchedule(staff, shifts);
            copy.schedule.clear();
            for (Shift shift : this.schedule) {
                copy.schedule.add(shift.copy());
            }
            return copy;
        }

        public double getFitness() { return fitness; }
        public List<Shift> getShifts() { return shifts; }
    }

    public static class ResourceAllocation {
        private final List<Resource> resources;
        private final List<Task> tasks;
        private final Map<Task, Resource> allocation = new HashMap<>();

        public ResourceAllocation(List<Resource> resources, List<Task> tasks) {
            this.resources = resources;
            this.tasks = tasks;
        }

        public void assign(Resource resource, Task task) {
            allocation.put(task, resource);
        }

        public double getEfficiency() {
            double total = 0;
            for (Map.Entry<Task, Resource> entry : allocation.entrySet()) {
                total += entry.getValue().getEfficiency(entry.getKey());
            }
            return total / Math.max(1, allocation.size());
        }
    }

    // Simple data classes
    public static class Complaint {
        public Long id;
        public String description;
        public String category;
        public int priority;
    }

    public static class Admin {
        public Long id;
        public String name;
        public String expertise;
        public double workload = 0;
        public double getWorkload() { return workload; }
    }

    public static class Staff {
        public Long id;
        public String name;
        public String skill;
        public int maxHours = 40;
    }

    public static class Shift {
        public Long id;
        public String time;
        public Staff assignedStaff;
        public Shift copy() { 
            Shift s = new Shift(); 
            s.id = this.id;
            s.time = this.time;
            s.assignedStaff = this.assignedStaff;
            return s;
        }
        public Staff getAssignedStaff() { return assignedStaff; }
        public void setAssignedStaff(Staff staff) { this.assignedStaff = staff; }
    }

    public static class Resource {
        public Long id;
        public String name;
        public double capacity = 100;
        public double getCost(Task task) { return 1.0; }
        public double getCapacity() { return capacity; }
        public void reduceCapacity(double amount) { capacity -= amount; }
        public double getEfficiency(Task task) { return 0.8; }
    }

    public static class Task {
        public Long id;
        public String name;
        public double requirement = 10;
        public double getRequirement() { return requirement; }
    }
}