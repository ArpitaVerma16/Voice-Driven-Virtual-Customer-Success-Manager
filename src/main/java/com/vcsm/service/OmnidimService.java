package com.vcsm.service;

import com.vcsm.model.VoiceCommand;
import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.model.Event;
import com.vcsm.repository.VoiceCommandRepository;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

@Service
public class OmnidimService {

    private static final Logger log = LoggerFactory.getLogger(OmnidimService.class);

    @Value("${omnidim.api.key:YOUR_OMNIDIM_API_KEY}")
    private String apiKey;

    @Autowired
    private VoiceCommandRepository voiceCommandRepository;

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRegistrationService eventRegistrationService;

    @Autowired
    private VoiceModelRegistryService voiceModelRegistryService;

    @Autowired
    private VoiceAnalyticsService voiceAnalyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchedulingOptimizer schedulingOptimizer;

    private final Map<Long, PendingBookingState> pendingBookings = new java.util.concurrent.ConcurrentHashMap<>();

    public static class PendingBookingState {
        private final String venueName;
        private final LocalDateTime requestedStart;
        private final LocalDateTime requestedEnd;
        private final List<LocalDateTime[]> alternatives;

        public PendingBookingState(String venueName, LocalDateTime requestedStart, LocalDateTime requestedEnd, List<LocalDateTime[]> alternatives) {
            this.venueName = venueName;
            this.requestedStart = requestedStart;
            this.requestedEnd = requestedEnd;
            this.alternatives = alternatives;
        }

        public String getVenueName() { return venueName; }
        public LocalDateTime getRequestedStart() { return requestedStart; }
        public LocalDateTime getRequestedEnd() { return requestedEnd; }
        public List<LocalDateTime[]> getAlternatives() { return alternatives; }
    }

    public Map<String, Object> processVoiceCommand(String transcript) {
        long startTime = System.currentTimeMillis();
        
        log.info("Processing: " + transcript);
        String lower = transcript.toLowerCase();
        String intent = detectIntent(lower);
        String response = switch (intent) {
            case "FILE_COMPLAINT"      -> handleComplaintVoice(lower);
            case "CHECK_COMPLAINT"     -> handleStatusCheck();
            case "EVENT_QUERY"         -> handleEventQuery();
            case "CANCEL_REGISTRATION" -> handleCancelRegistration(lower);
            case "ANALYTICS"           -> handleAnalytics();
            default -> "I'm your Virtual Community Manager. I can help with complaints, events, and analytics!";
        };

        long responseTime = System.currentTimeMillis() - startTime;

        VoiceCommand cmd = new VoiceCommand();
        cmd.setTranscript(transcript);
        cmd.setIntent(intent);
        cmd.setResponse(response);
        cmd.setProcessed(true);
        voiceCommandRepository.save(cmd);

        // Log voice analytics
        try {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth != null ? auth.getName() : null;
            user = null;
            if (email != null) {
                user = userRepository.findByEmail(email).orElse(null);
            }
            if (user == null) {
                user = userRepository.findById(1L).orElse(null); // Fallback to 1L
            }

            if (user != null) {
                boolean success = !intent.equals("UNKNOWN");
                voiceAnalyticsService.logCommand(user, transcript, intent, success, responseTime);
            }
        } catch (Exception e) {
            log.warn("Failed to log voice analytics: {}", e.getMessage(), e);
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("intent", intent);
        result.put("transcript", transcript);
        result.put("response", response);
        result.put("success", true);
        result.put("responseTime", responseTime);
        voiceModelRegistryService.getActiveModel()
                .ifPresent(model -> result.put("voiceModelKey", model.modelKey()));
        return result;
    }

    private String detectIntent(String t) {
        if (t.contains("book") || t.contains("reserve") || t.contains("schedule")) {
            if (t.contains("hall") || t.contains("clubhouse") || t.contains("gym") || t.contains("venue")) {
                return "BOOK_VENUE";
            }
        }
        if (t.contains("status") || t.contains("check") || t.contains("my complaint")) return "CHECK_COMPLAINT";
        if (t.contains("complaint") || t.contains("noise") || t.contains("maintenance")
                || t.contains("broken") || t.contains("security") || t.contains("parking")) return "FILE_COMPLAINT";
        if (t.contains("cancel") || t.contains("opt out") || t.contains("withdraw")
                || t.contains("un-register") || t.contains("unregister")) return "CANCEL_REGISTRATION";
        if (t.contains("event") || t.contains("sports") || t.contains("cultural")
                || t.contains("activity")) return "EVENT_QUERY";
        if (t.contains("analytics") || t.contains("how many") || t.contains("total")
                || t.contains("summary")) return "ANALYTICS";
        return "UNKNOWN";
    }

    private String handleCancelRegistration(String t) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? auth.getName() : null;
        User user = null;
        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null) {
            user = userRepository.findById(1L).orElse(null); // Fallback to 1L
        }

        if (user == null) {
            return "User not found. Please log in first.";
        }

        List<Event> userEvents = eventRegistrationService.getUserEvents(user);
        if (userEvents.isEmpty()) {
            return "You are not registered for any upcoming events.";
        }

        Event matchedEvent = null;
        for (Event e : userEvents) {
            if (t.contains(e.getName().toLowerCase())) {
                matchedEvent = e;
                break;
            }
        }

        if (matchedEvent == null) {
            List<Event> activeEvents = eventService.getActiveEvents();
            for (Event e : activeEvents) {
                if (t.contains(e.getName().toLowerCase())) {
                    matchedEvent = e;
                    break;
                }
            }
        }

        if (matchedEvent == null) {
            if (userEvents.size() == 1) {
                matchedEvent = userEvents.get(0);
            } else {
                return "Which event registration would you like to cancel? Please specify the event name.";
            }
        }

        try {
            eventRegistrationService.cancelRegistration(matchedEvent, user);
            return "Successfully cancelled your registration for the event: " + matchedEvent.getName();
        } catch (Exception e) {
            return "Failed to cancel registration: " + e.getMessage();
        }
    }

    private String handleComplaintVoice(String t) {
        String cat = "general";
        if (t.contains("noise")) cat = "noise";
        else if (t.contains("maintenance") || t.contains("broken")) cat = "maintenance";
        else if (t.contains("security")) cat = "security";
        else if (t.contains("parking")) cat = "parking";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? auth.getName() : null;
        User user = null;
        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null) {
            user = userRepository.findById(1L).orElse(null); // Fallback to 1L
        }

        Complaint complaint = new Complaint();
        if (user != null) {
            complaint.setResidentName(user.getName());
            complaint.setResidentUsername(user.getEmail());
            complaint.setContactEmail(user.getEmail());
            complaint.setUser(user);
        } else {
            complaint.setResidentName("Voice Command");
        }
        complaint.setDescription(t);
        complaint.setCategory(Complaint.ComplaintCategory.valueOf(cat.toUpperCase()));
        complaintService.fileComplaint(complaint);

        return "Complaint filed successfully for " + cat + " issue. Reference ID: " + complaint.getId();
    }

    private String handleStatusCheck() {
        Map<String, Long> s = complaintService.getComplaintStats();
        return "Currently " + s.get("open") + " open complaints and " + s.get("inProgress") + " in progress.";
    }

    private String handleEventQuery() {
        var upcoming = eventService.getUpcomingEvents();
        if (upcoming.isEmpty()) return "No upcoming events right now. Check back soon!";
        StringBuilder sb = new StringBuilder("Upcoming: ");
        upcoming.stream().limit(3).forEach(e -> sb.append(e.getName()).append(", "));
        return sb.toString().replaceAll(", $", ". Visit Events section for details!");
    }

    private String handleAnalytics() {
        Map<String, Long> s = complaintService.getComplaintStats();
        return "Summary: " + s.get("total") + " complaints (" + s.get("open") + " open, "
                + s.get("resolved") + " resolved). " + eventService.getActiveEvents().size() + " active events.";
    }

    public List<VoiceCommand> getRecentCommands() {
        return voiceCommandRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public List<VoiceCommand> getRecentCommands(Boolean success) {

        if (success == null) {
            return voiceCommandRepository.findTop10ByOrderByCreatedAtDesc();
        }

        return voiceCommandRepository.findByProcessedOrderByCreatedAtDesc(success);
    private String handleEventBooking(String t) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? auth.getName() : null;
        User user = null;
        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null) {
            user = userRepository.findById(1L).orElse(null); // Fallback to 1L
        }
        if (user == null) {
            return "Unable to book event: user session not found.";
        }

        List<Event> events = eventService.getActiveEvents();
        Event matchedEvent = null;
        
        for (Event event : events) {
            String eventName = event.getName().toLowerCase();
            if (t.contains(eventName)) {
                matchedEvent = event;
                break;
            }
        }

        if (matchedEvent == null) {
            for (Event event : events) {
                String[] words = event.getName().toLowerCase().split("\\s+");
                int matchCount = 0;
                for (String word : words) {
                    if (word.length() > 3 && t.contains(word)) {
                        matchCount++;
                    }
                }
                if (matchCount > 0) {
                    matchedEvent = event;
                    break;
                }
            }
        }

        if (matchedEvent == null) {
            return "Sorry, I couldn't find an event matching that description. Please try specifying the exact event name.";
        }

        try {
            Event updatedEvent = eventService.registerForEvent(matchedEvent.getId(), user.getId());
            return "Success! You have been registered for " + updatedEvent.getName() + ". A confirmation email with your ticket check-in QR code has been sent to " + user.getEmail() + ".";
        } catch (Exception e) {
            return "Could not complete booking: " + e.getMessage();
        }
    }
}