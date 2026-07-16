package com.vcsm.controller.graphql;

import com.vcsm.model.Event;
import com.vcsm.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class EventResolver {

    private final EventService eventService;

    @QueryMapping
    public List<Event> events(
        @Argument Boolean upcoming,
        @Argument EventCategory category
    ) {
        if (upcoming != null && upcoming) {
            return eventService.getUpcomingEvents();
        }
        if (category != null) {
            return eventService.getEventsByCategory(category);
        }
        return eventService.getAllEvents();
    }

    @QueryMapping
    public Event event(@Argument Long id) {
        return eventService.getEventById(id)
            .orElseThrow(() -> new RuntimeException("Event not found: " + id));
    }

    @QueryMapping
    public List<Event> upcomingEvents(@Argument Integer limit) {
        List<Event> events = eventService.getUpcomingEvents();
        if (limit != null && limit > 0) {
            return events.stream().limit(limit).toList();
        }
        return events;
    }

    @QueryMapping
    public List<Event> recommendedEvents(@Argument String keyword) {
        return eventService.getRecommendedEvents(keyword);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Event createEvent(@Argument CreateEventInput input) {
        Event event = new Event();
        event.setName(input.name());
        event.setDescription(input.description());
        event.setCategory(Event.EventCategory.valueOf(input.category().name()));
        event.setLocation(input.location());
        event.setEventDate(input.eventDate());
        event.setMaxCapacity(input.maxCapacity());
        event.setOrganizer(input.organizer());
        return eventService.createEvent(event);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Event registerForEvent(@Argument Long eventId) {
        return eventService.registerForEvent(eventId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean cancelEventRegistration(@Argument Long eventId) {
        eventService.cancelRegistration(eventId);
        return true;
    }

    // DTOs
    public record CreateEventInput(
        String name,
        String description,
        EventCategory category,
        String location,
        LocalDateTime eventDate,
        Integer maxCapacity,
        String organizer
    ) {}

    public enum EventCategory {
        SOCIAL, SPORTS, CULTURAL, EDUCATIONAL, HEALTH, OTHER
    }
}