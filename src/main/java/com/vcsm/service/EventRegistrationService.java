package com.vcsm.service;

import com.vcsm.model.Event;
import com.vcsm.model.User;
import com.vcsm.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventRegistrationService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ReminderScheduler reminderScheduler;

    /**
     * Register a user for an event
     */
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
        if (event.getRegisteredUsers() != null && event.getRegisteredUsers().contains(user)) {
            throw new RuntimeException("User already registered for this event");
        }

        // Add user to event registrations
        if (event.getRegisteredUsers() == null) {
            event.setRegisteredUsers(new ArrayList<>());
        }
        event.getRegisteredUsers().add(user);
        event.setRegistrations(event.getRegistrations() + 1);

        Event updatedEvent = eventRepository.save(event);

        // Send confirmation email
        reminderScheduler.sendRegistrationConfirmation(updatedEvent, user);

        return updatedEvent;
    }

    /**
     * Cancel registration for an event
     */
    public Event cancelRegistration(Event event, User user) {
        if (event == null) {
            throw new RuntimeException("Event not found");
        }

        if (event.getRegisteredUsers() != null && event.getRegisteredUsers().contains(user)) {
            event.getRegisteredUsers().remove(user);
            event.setRegistrations(event.getRegistrations() - 1);
            return eventRepository.save(event);
        }

        throw new RuntimeException("User not registered for this event");
    }

    /**
     * Check if user is registered for event
     */
    public boolean isUserRegistered(Event event, User user) {
        return event.getRegisteredUsers() != null && event.getRegisteredUsers().contains(user);
    }

    /**
     * Get all registrants for an event
     */
    public List<User> getEventRegistrants(Event event) {
        return event.getRegisteredUsers() != null ? event.getRegisteredUsers() : new ArrayList<>();
    }

    /**
     * Get all events a user is registered for
     */
    public List<Event> getUserEvents(User user) {
        return eventRepository.findByRegisteredUsersContaining(user);
    }

    /**
     * Get registration count for an event
     */
    public int getRegistrationCount(Event event) {
        return event.getRegistrations();
    }
}