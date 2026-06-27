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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.vcsm.model.VenueReservation;

@Service
public class OmnidimService {

    private static final Logger log = Logger.getLogger(OmnidimService.class.getName());

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
            log.warning("Failed to log voice analytics: " + e.getMessage());
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

    private String handleBookVenue(User user, String transcript) {
        String venueName = "Venue";
        if (transcript.contains("clubhouse")) venueName = "Clubhouse";
        else if (transcript.contains("gym")) venueName = "Gym";
        else if (transcript.contains("hall") || transcript.contains("community hall")) venueName = "Community Hall";
        
        LocalDateTime[] times = parseBookingTime(transcript);
        LocalDateTime start = times[0];
        LocalDateTime end = times[1];
        
        if (schedulingOptimizer.hasConflict(venueName, start, end)) {
            List<LocalDateTime[]> alternatives = schedulingOptimizer.findAlternatives(venueName, start, end);
            if (alternatives.isEmpty()) {
                return "The requested slot is occupied, and no alternatives were found in the near future.";
            }
            
            if (user != null) {
                pendingBookings.put(user.getId(), new PendingBookingState(venueName, start, end, alternatives));
            }
            
            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a");
            StringBuilder sb = new StringBuilder("That slot is taken, but I can book you at ");
            for (int i = 0; i < alternatives.size(); i++) {
                sb.append(alternatives.get(i)[0].format(timeFormatter));
                if (i < alternatives.size() - 1) {
                    sb.append(" or ");
                }
            }
            sb.append(" instead. Which do you prefer?");
            return sb.toString();
        } else {
            schedulingOptimizer.bookVenue(venueName, user, start, end);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");
            return "Successfully booked " + venueName + " from " + start.format(formatter) + " to " + end.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")) + ".";
        }
    }

    private LocalDateTime[] parseBookingTime(String t) {
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(1);
        if (t.contains("today")) {
            date = java.time.LocalDate.now();
        } else if (t.contains("tomorrow")) {
            date = java.time.LocalDate.now().plusDays(1);
        } else if (t.contains("monday")) {
            date = getNextDayOfWeek(java.time.DayOfWeek.MONDAY);
        }
        
        int startHour = 14; 
        int startMin = 0;
        int durationMinutes = 60; 
        
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)(:(\\d+))?\\s*(am|pm)").matcher(t);
        if (m.find()) {
            startHour = Integer.parseInt(m.group(1));
            if (m.group(3) != null) {
                startMin = Integer.parseInt(m.group(3));
            }
            String ampm = m.group(4);
            if ("pm".equalsIgnoreCase(ampm) && startHour < 12) {
                startHour += 12;
            } else if ("am".equalsIgnoreCase(ampm) && startHour == 12) {
                startHour = 0;
            }
        }
        
        java.util.regex.Matcher durMatcher = java.util.regex.Pattern.compile("for (\\d+)\\s*hour").matcher(t);
        if (durMatcher.find()) {
            durationMinutes = Integer.parseInt(durMatcher.group(1)) * 60;
        } else {
            java.util.regex.Matcher endMatcher = java.util.regex.Pattern.compile("(?:to|until) (\\d+)(:(\\d+))?\\s*(am|pm)").matcher(t);
            if (endMatcher.find()) {
                int endHour = Integer.parseInt(endMatcher.group(1));
                int endMin = 0;
                if (endMatcher.group(3) != null) {
                    endMin = Integer.parseInt(endMatcher.group(3));
                }
                String endAmpm = endMatcher.group(4);
                if ("pm".equalsIgnoreCase(endAmpm) && endHour < 12) {
                    endHour += 12;
                } else if ("am".equalsIgnoreCase(endAmpm) && endHour == 12) {
                    endHour = 0;
                }
                LocalDateTime startTemp = date.atTime(startHour, startMin);
                LocalDateTime endTemp = date.atTime(endHour, endMin);
                if (endTemp.isAfter(startTemp)) {
                    durationMinutes = (int) java.time.temporal.ChronoUnit.MINUTES.between(startTemp, endTemp);
                }
            }
        }
        
        LocalDateTime start = date.atTime(startHour, startMin);
        LocalDateTime end = start.plusMinutes(durationMinutes);
        return new LocalDateTime[]{start, end};
    }
    
    private java.time.LocalDate getNextDayOfWeek(java.time.DayOfWeek dayOfWeek) {
        java.time.LocalDate d = java.time.LocalDate.now();
        while (d.getDayOfWeek() != dayOfWeek) {
            d = d.plusDays(1);
        }
        return d;
    }
    
    private String handleFollowUp(User user, PendingBookingState pending, String transcript) {
        String lower = transcript.toLowerCase();
        
        if (lower.contains("cancel") || lower.contains("no") || lower.contains("stop")) {
            pendingBookings.remove(user.getId());
            return "Booking operation cancelled.";
        }
        
        int selectedIndex = -1;
        if (lower.contains("first") || lower.contains("1st") || lower.contains("one") || lower.contains("option 1")) {
            selectedIndex = 0;
        } else if (lower.contains("second") || lower.contains("2nd") || lower.contains("two") || lower.contains("option 2")) {
            selectedIndex = 1;
        } else {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*(am|pm)").matcher(lower);
            if (m.find()) {
                int hour = Integer.parseInt(m.group(1));
                String ampm = m.group(2);
                if ("pm".equalsIgnoreCase(ampm) && hour < 12) hour += 12;
                else if ("am".equalsIgnoreCase(ampm) && hour == 12) hour = 0;
                
                List<LocalDateTime[]> alternatives = pending.getAlternatives();
                for (int i = 0; i < alternatives.size(); i++) {
                    if (alternatives.get(i)[0].getHour() == hour) {
                        selectedIndex = i;
                        break;
                    }
                }
            }
        }
        
        if (selectedIndex >= 0 && selectedIndex < pending.getAlternatives().size()) {
            LocalDateTime[] slot = pending.getAlternatives().get(selectedIndex);
            pendingBookings.remove(user.getId());
            
            schedulingOptimizer.bookVenue(pending.getVenueName(), user, slot[0], slot[1]);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");
            return "Successfully booked " + pending.getVenueName() + " from " + slot[0].format(formatter) + " to " + slot[1].format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")) + ".";
        }
        
        pendingBookings.remove(user.getId());
        return null;
    }
}