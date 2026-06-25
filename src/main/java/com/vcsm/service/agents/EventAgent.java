package com.vcsm.service.agents;

import com.vcsm.model.Event;
import com.vcsm.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EventAgent {

    @Autowired
    private EventService eventService;

    public Map<String, Object> process(String query, Long userId) {
        Map<String, Object> response = new HashMap<>();

        if (query.toLowerCase().contains("upcoming") || query.toLowerCase().contains("show")) {
            List<Event> upcoming = eventService.getUpcomingEvents();
            
            response.put("success", true);
            response.put("action", "show_events");
            response.put("message", "Here are upcoming events:");
            response.put("events", upcoming.stream().limit(5).toList());
            
        } else if (query.toLowerCase().contains("register")) {
            // Simplified registration
            response.put("success", true);
            response.put("action", "event_registration");
            response.put("message", "I can help you register for an event. Please provide the event ID.");
            
        } else {
            response.put("success", true);
            response.put("action", "event_query");
            response.put("message", "I can help you find and register for events. Try asking about upcoming events.");
        }

        return response;
    }
}