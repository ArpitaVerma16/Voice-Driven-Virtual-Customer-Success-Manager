package com.vcsm.controller;

import com.vcsm.model.Complaint;
import com.vcsm.service.ComplaintService;
import com.vcsm.service.EventService;
import com.vcsm.service.InteractionService;
import com.vcsm.service.OmnidimService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@lombok.RequiredArgsConstructor
public class WebController {

    private final ComplaintService complaintService;

    private final EventService eventService;

    private final OmnidimService omnidimService;

    private final InteractionService interactionService;

    private final com.vcsm.repository.UserRepository userRepository;

    @org.springframework.web.bind.annotation.ModelAttribute("userRole")
    public String getUserRole(org.springframework.security.core.Authentication auth) {
        if (auth == null) return "ROLE_USER";
        return auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .filter(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_AUDITOR"))
                .findFirst()
                .orElse("ROLE_USER");
    }

    @GetMapping("/landing")
    public String landing() {
        return org.springframework.http.ResponseEntity.ok("landing");
    }

    @GetMapping("/login")
    public String login() {
        return org.springframework.http.ResponseEntity.ok("login");
    }

    @GetMapping("/chatbot")
    public String chatbot() {
        return org.springframework.http.ResponseEntity.ok("chatbot-ui");
    }

    @GetMapping("/voice-templates")
    public String voiceTemplates() {
        return org.springframework.http.ResponseEntity.ok("voice-templates");
    }

    @GetMapping("/profile")
    public String profile() {
        return org.springframework.http.ResponseEntity.ok("profile");
    }

    @GetMapping("/onboarding")
    public String onboarding() {
        return org.springframework.http.ResponseEntity.ok("onboarding");
    }

    @GetMapping("/voice-analytics")
    public String voiceAnalytics() {
        return org.springframework.http.ResponseEntity.ok("voice-analytics");
    }

    @GetMapping("/audit-logs")
    public String auditLogs() {
        return org.springframework.http.ResponseEntity.ok("audit-logs");
    }

    @GetMapping("/ivr-builder")
    public String ivrBuilder() {
        return org.springframework.http.ResponseEntity.ok("ivr-builder");
    }

    @GetMapping("/")
    public String dashboard(Model model) {

        Map<String, Long> stats = complaintService.getComplaintStats();

        if (stats == null) {
            stats = new HashMap<>();
            stats.put("open", 0L);
            stats.put("inProgress", 0L);
            stats.put("resolved", 0L);
        }

        // Recent complaints via a LIMIT 5 query instead of loading the whole
        // complaints table; page load time no longer scales with table size.
        List<?> recentComplaints = complaintService.getRecentComplaints(5);

        if (recentComplaints == null) {
            recentComplaints = new ArrayList<>();
        }

        List<?> commands = omnidimService.getRecentCommands();

        if (commands == null) {
            commands = new ArrayList<>();
        }

        model.addAttribute("complaintStats", stats);

        // Dashboard cards only need counts: COUNT(*) in the database instead
        // of materializing every event row (twice, previously).
        model.addAttribute("activeEvents", eventService.countActiveEvents());

        model.addAttribute("upcomingEvents", eventService.countUpcomingEvents());

        model.addAttribute("recentComplaints", recentComplaints);

        model.addAttribute("recentCommands",
                commands.stream().limit(5).toList());

        // High-risk residents filtered in the database rather than streaming
        // the entire users table into memory.
        List<com.vcsm.model.User> highRiskUsers =
                userRepository.findByDissatisfactionScoreGreaterThanEqual(75.0);
        model.addAttribute("highRiskUsers", highRiskUsers);

        return org.springframework.http.ResponseEntity.ok("dashboard");
    }

    @GetMapping("/complaints")
    public String complaintsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDateTime.parse(startDate + "T00:00:00");
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDateTime.parse(endDate + "T23:59:59");
            }
        } catch (SpecificException e) {
            // Ignore date parsing errors
        }

        Page<Complaint> complaintPage = complaintService.searchComplaints(
            keyword, status, category, priority, start, end, pageable);

        model.addAttribute("complaints", complaintPage.getContent());
        model.addAttribute("page", complaintPage);
        model.addAttribute("stats", complaintService.getComplaintStats());

        return org.springframework.http.ResponseEntity.ok("complaints");
    }

    @GetMapping("/events")
    public String events(Model model) {

        model.addAttribute("events",
                eventService.getAllEvents() != null
                        ? eventService.getAllEvents()
                        : new ArrayList<>());

        model.addAttribute("upcomingCount",
                eventService.getUpcomingEvents() != null
                        ? eventService.getUpcomingEvents().size()
                        : 0);

        return org.springframework.http.ResponseEntity.ok("events");
    }

    @GetMapping("/voice-cloning")
    public String voiceCloning() {
       return org.springframework.http.ResponseEntity.ok("voice-cloning-ui");
    }

    @GetMapping("/live-dashboard")
    public String liveDashboard() {
        return org.springframework.http.ResponseEntity.ok("live-dashboard");
    }

    @GetMapping("/translation")
    public String translation() {
        return org.springframework.http.ResponseEntity.ok("translation-ui");
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {

        model.addAttribute("complaintStats",
                complaintService.getComplaintStats() != null
                        ? complaintService.getComplaintStats()
                        : new HashMap<>());

        model.addAttribute("categoryStats",
                complaintService.getComplaintsByCategory() != null
                        ? complaintService.getComplaintsByCategory()
                        : new HashMap<>());

        model.addAttribute("totalEvents",
                eventService.getAllEvents() != null
                        ? eventService.getAllEvents().size()
                        : 0);

        model.addAttribute("activeEvents",
                eventService.getActiveEvents() != null
                        ? eventService.getActiveEvents().size()
                        : 0);

        return org.springframework.http.ResponseEntity.ok("analytics");
    }

    @GetMapping("/blockchain-verify")
    public String blockchainVerify() {
        return org.springframework.http.ResponseEntity.ok("blockchain-verify");
    }

    @GetMapping("/offline")
    public String offline() {
        return org.springframework.http.ResponseEntity.ok("offline");
    }

    @GetMapping("/twilio-demo")
    public String twilioDemo() {
        return org.springframework.http.ResponseEntity.ok("twilio-demo");
    }

    @GetMapping("/interaction-history")
    public String interactionHistory(Model model) {
        try {
            Map<String, Long> stats = interactionService.getInteractionStats();
            if (stats == null) {
                stats = new HashMap<>();
                stats.put("total", 0L);
                stats.put("completed", 0L);
                stats.put("pending", 0L);
                stats.put("inProgress", 0L);
                stats.put("positive", 0L);
                stats.put("neutral", 0L);
                stats.put("negative", 0L);
            }
            model.addAttribute("interactionStats", stats);
        } catch (SpecificException e) {
            model.addAttribute("interactionStats", new HashMap<>());
        }
        return org.springframework.http.ResponseEntity.ok("interaction-history");
    }

}