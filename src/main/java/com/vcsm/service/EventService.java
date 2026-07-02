package com.vcsm.service;

import com.vcsm.exception.EventCapacityExceededException;
import com.vcsm.model.Event;
import com.vcsm.model.User;
import com.vcsm.model.EventRegistration;
import com.vcsm.repository.EventRepository;
import com.vcsm.repository.UserRepository;
import com.vcsm.repository.EventRegistrationRepository;
import com.vcsm.repository.EventWaitlistRepository;
import com.vcsm.repository.EmailLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@lombok.RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final EventRegistrationRepository eventRegistrationRepository;

    private final EventWaitlistRepository eventWaitlistRepository;

    private final EmailLogRepository emailLogRepository;

    @Transactional
    public Event createEvent(Event event) { return eventRepository.save(event); }

    public List<Event> getAllEvents() { return eventRepository.findAll(); }

    public List<Event> getActiveEvents() { return eventRepository.findByActiveTrue(); }

    public List<Event> getUpcomingEvents() {
        return eventRepository.findByEventDateAfterAndActiveTrue(LocalDateTime.now());
    }

    public List<Event> getEventsByCategory(Event.EventCategory category) {
        return eventRepository.findByCategoryAndActiveTrue(category);
    }

    public Optional<Event> getEventById(Long id) { return eventRepository.findById(id); }

    @Transactional
    public Event updateEvent(Long id, Event updated) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
        event.setName(updated.getName());
        event.setDescription(updated.getDescription());
        event.setCategory(updated.getCategory());
        event.setLocation(updated.getLocation());
        event.setEventDate(updated.getEventDate());
        event.setMaxCapacity(updated.getMaxCapacity());
        return eventRepository.save(event);
    }

    @Transactional
    public Event registerForEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Event inactive
        if (!event.isActive()) {
            throw new RuntimeException("Event is not active");
        }
        // Event already happened
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Registration closed. Event already started.");
        }

        // Check if already registered
        if (eventRegistrationRepository.existsByUserAndEvent(user, event)) {
            throw new RuntimeException("User already registered for this event");
        }

        // Capacity validation
        if (event.getRegistrations() >= event.getMaxCapacity()) {
            throw new EventCapacityExceededException(
                "Event has reached its maximum capacity of "
                    + event.getMaxCapacity()
                    + " participants."
            );
        }

        // Create and save event registration
        EventRegistration registration = new EventRegistration(user, event);
        registration = eventRegistrationRepository.save(registration);

        // Generate signed ticket token
        String token = jwtService.generateTicketToken(registration.getId(), user.getId(), event.getId());
        registration.setTicketToken(token);
        eventRegistrationRepository.save(registration);

        event.setRegistrations(event.getRegistrations() + 1);
        Event savedEvent = eventRepository.save(event);

        // Send confirmation email
        try {
            reminderScheduler.sendRegistrationConfirmation(savedEvent, user);
        } catch (Exception e) {
            System.err.println("❌ Failed to send registration email: " + e.getMessage());
        }

        return savedEvent;
    }

    public List<Event> recommendEvents(String keyword) {
        try {
            return eventRepository.findByCategoryAndActiveTrue(Event.EventCategory.valueOf(keyword.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return eventRepository.findByActiveTrue();
        }
    }

    @Transactional
    public void deleteEvent(Long id) {
        Optional<Event> eventOpt = eventRepository.findById(id);
        if (eventOpt.isPresent()) {
            Event event = eventOpt.get();
            eventWaitlistRepository.deleteByEvent(event);
            emailLogRepository.deleteByEvent(event);
            eventRegistrationRepository.deleteByEvent(event);
            eventRepository.delete(event);
        }
    }
}