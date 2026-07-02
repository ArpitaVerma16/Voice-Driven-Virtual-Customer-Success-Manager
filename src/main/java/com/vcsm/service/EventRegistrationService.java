package com.vcsm.service;

import com.vcsm.model.Event;
import com.vcsm.model.User;
import com.vcsm.model.EventRegistration;
import com.vcsm.repository.EventRepository;
import com.vcsm.repository.EventRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@lombok.RequiredArgsConstructor
public class EventRegistrationService {

    private final EventRepository eventRepository;

    private final EventRegistrationRepository eventRegistrationRepository;

    private final ReminderScheduler reminderScheduler;

    private final com.vcsm.security.jwt.JwtService jwtService;

    /**
     * Register a user for an event
     */
    @Transactional
    public Event registerUserForEvent(Event event, User user) {
        // Check if event exists
        if (event == null) {
            throw new RuntimeException("Event not found");
        }

        // Check if event is full
        if (event.getRegistrations() >= event.getMaxCapacity()) {
            throw new RuntimeException("Event is full");
        }

        // Check if user already registered
        if (eventRegistrationRepository.existsByUserAndEvent(user, event)) {
            throw new RuntimeException("User already registered for this event");
        }

        // Create and save event registration
        EventRegistration registration = new EventRegistration(user, event);
        registration = eventRegistrationRepository.save(registration);

        // Generate signed ticket token
        String token = jwtService.generateTicketToken(registration.getId(), user.getId(), event.getId());
        registration.setTicketToken(token);
        eventRegistrationRepository.save(registration);

        event.setRegistrations(event.getRegistrations() + 1);

        Event updatedEvent = eventRepository.save(event);

        // Send confirmation email
        reminderScheduler.sendRegistrationConfirmation(updatedEvent, user);

        return updatedEvent;
    }

    /**
     * Cancel registration for an event
     */
    @Transactional
    public Event cancelRegistration(Event event, User user) {
        if (event == null) {
            throw new RuntimeException("Event not found");
        }

        java.util.Optional<EventRegistration> registrationOpt = eventRegistrationRepository.findByUserAndEvent(user, event);
        if (registrationOpt.isPresent()) {
            eventRegistrationRepository.delete(registrationOpt.get());
            event.setRegistrations(event.getRegistrations() - 1);
            return eventRepository.save(event);
        }

        throw new RuntimeException("User not registered for this event");
    }

    /**
     * Check if user is registered for event
     */
    public boolean isUserRegistered(Event event, User user) {
        if (event == null || user == null) {
            return false;
        }
        return eventRegistrationRepository.existsByUserAndEvent(user, event);
    }

    /**
     * Get all registrants for an event
     */
    public List<User> getEventRegistrants(Event event) {
        if (event == null) {
            return new ArrayList<>();
        }
        return eventRegistrationRepository.findByEvent(event).stream()
                .map(EventRegistration::getUser)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get all events a user is registered for
     */
    public List<Event> getUserEvents(User user) {
        if (user == null) {
            return new ArrayList<>();
        }
        return eventRegistrationRepository.findByUser(user).stream()
                .map(EventRegistration::getEvent)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get registration count for an event
     */
    public int getRegistrationCount(Event event) {
        return event.getRegistrations();
    }
}