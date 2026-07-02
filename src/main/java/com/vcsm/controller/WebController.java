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
public class WebController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private EventService eventService;

    @Autowired
    private OmnidimService omnidimService;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private com.vcsm.repository.UserRepository userRepository;

    @GetMapping("/landing")
    public String landing() {
        return "landing";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/chatbot")
    public String chatbot() {
        return "chatbot-ui";
    }

    @GetMapping("/voice-templates")
    public String voiceTemplates() {
        return "voice-templates";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/onboarding")
    public String onboarding() {
        return "onboarding";
    }

    @GetMapping("/voice-analytics")
    public String voiceAnalytics() {
        return "voice-analytics";
    }

    @GetMapping("/audit-logs")
    public String auditLogs() {
        return "audit-logs";
    }

    @GetMapping("/ivr-builder")
    public String ivrBuilder() {
        return "ivr-builder";
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

        List<?> complaints = complaintService.getAllComplaints();

        if (complaints == null) {
            complaints = new ArrayList<>();
        }

        List<?> commands = omnidimService.getRecentCommands();

        if (commands == null) {
            commands = new ArrayList<>();
        }

        model.addAttribute("complaintStats", stats);

        model.addAttribute("activeEvents",
                eventService.getActiveEvents() != null
                        ? eventService.getActiveEvents().size()
                        : 0);

        model.addAttribute("upcomingEvents",
                eventService.getUpcomingEvents() != null
                        ? eventService.getUpcomingEvents().size()
                        : 0);

        model.addAttribute("recentComplaints",
                complaints.stream().limit(5).toList());

        model.addAttribute("recentCommands",
                commands.stream().limit(5).toList());

        List<com.vcsm.model.User> highRiskUsers = userRepository.findAll() /* filtered */.stream()
                .filter(u -> u.getDissatisfactionScore() >= 75.0)
                .toList();
        model.addAttribute("highRiskUsers", highRiskUsers);

        return "dashboard";
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
        } catch (Exception e) {
            // Ignore date parsing errors
        }

        Page<Complaint> complaintPage = complaintService.searchComplaints(
            keyword, status, category, priority, start, end, pageable);

        model.addAttribute("complaints", complaintPage.getContent());
        model.addAttribute("page", complaintPage);
        model.addAttribute("stats", complaintService.getComplaintStats());

        return "complaints";
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

        return "events";
    }

    @GetMapping("/voice-cloning")
    public String voiceCloning() {
       return "voice-cloning-ui";
    }

    @GetMapping("/live-dashboard")
    public String liveDashboard() {
        return "live-dashboard";
    }

    @GetMapping("/translation")
    public String translation() {
        return "translation-ui";
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

        return "analytics";
    }

    @GetMapping("/blockchain-verify")
    public String blockchainVerify() {
        return "blockchain-verify";
    }

    @GetMapping("/offline")
    public String offline() {
        return "offline";
    }

    @GetMapping("/twilio-demo")
    public String twilioDemo() {
        return "twilio-demo";
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
        } catch (Exception e) {
            model.addAttribute("interactionStats", new HashMap<>());
        }
        return "interaction-history";
    }

}