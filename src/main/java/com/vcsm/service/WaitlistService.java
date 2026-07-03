package com.vcsm.service;

import com.vcsm.model.Event;
import com.vcsm.model.EventWaitlist;
import com.vcsm.model.User;
import com.vcsm.repository.EventWaitlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;

@Service
@lombok.RequiredArgsConstructor
public class WaitlistService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistService.class);
    
    private final EventWaitlistRepository waitlistRepository;
    
    private final EmailService emailService;
    
    private final EventRegistrationService eventRegistrationService;
    
    private final EventService eventService;
    
    /**
     * Add user to waitlist
     */
    @Transactional
    public EventWaitlist joinWaitlist(Event event, User user) {
        // Check if already on waitlist
        Optional<EventWaitlist> existing = waitlistRepository.findByEventAndUser(event, user);
        if (existing.isPresent()) {
            throw new RuntimeException("Already on waitlist");
        }
        
        // Check if event is actually full
        if (event.getRegistrations() < event.getMaxCapacity()) {
            throw new RuntimeException("Event has available slots. Please register directly.");
        }
        
        EventWaitlist waitlistEntry = new EventWaitlist(event, user);
        return waitlistRepository.save(waitlistEntry);
    }
    
    /**
     * Remove user from waitlist
     */
    @Transactional
    public void leaveWaitlist(Event event, User user) {
        waitlistRepository.deleteByEventAndUser(event, user);
    }
    
    /**
     * Get waitlist position for user
     */
    public int getWaitlistPosition(Event event, User user) {
        List<EventWaitlist> waitlist = waitlistRepository.findByEventOrderByJoinedAtAsc(event);
        for (int i = 0; i < waitlist.size(); i++) {
            if (waitlist.get(i).getUser().getId().equals(user.getId())) {
                return i + 1;
            }
        }
        return -1;
    }
    
    /**
     * Get waitlist count
     */
    public long getWaitlistCount(Event event) {
        return waitlistRepository.countByEventAndConfirmedFalse(event);
    }
    
    /**
     * Process waitlist when a slot opens up
     * Called when someone cancels registration
     */
    @Transactional
    public void processWaitlist(Event event) {
        long pendingUnexpired = waitlistRepository.countByEventAndConfirmedFalseAndExpiresAtAfter(event, LocalDateTime.now());
        long availableSlots = event.getMaxCapacity() - event.getRegistrations() - pendingUnexpired;
        
        if (availableSlots <= 0) {
            return;
        }
        
        for (int i = 0; i < availableSlots; i++) {
            Optional<EventWaitlist> nextOpt = waitlistRepository
                .findFirstByEventAndConfirmedFalseAndNotifiedAtIsNullOrderByJoinedAtAsc(event);
            if (nextOpt.isEmpty()) {
                break;
            }
            EventWaitlist entry = nextOpt.get();
            try {
                User user = entry.getUser();
                entry.setNotifiedAt(LocalDateTime.now());
                entry.setExpiresAt(LocalDateTime.now().plusHours(24));
                waitlistRepository.save(entry);
                emailService.sendEventSlotAvailable(event, user);
                log.info("Waitlist notification sent to user: {}", user.getEmail());
            } catch (SpecificException e) {
                log.error("Failed to process waitlist entry {}: {}", entry.getId(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Confirm waitlist position (called when user clicks confirmation link)
     */
    @Transactional
    public Event confirmWaitlist(Event event, User user) {
        Optional<EventWaitlist> waitlistEntry = waitlistRepository.findByEventAndUser(event, user);
        
        if (waitlistEntry.isEmpty()) {
            throw new RuntimeException("Not on waitlist");
        }
        
        EventWaitlist entry = waitlistEntry.get();
        
        // Check if expired
        if (entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Waitlist invitation expired");
        }
        
        // Check if already confirmed
        if (entry.isConfirmed()) {
            throw new RuntimeException("Already confirmed");
        }
        
        // Check if event still has slots
        if (event.getRegistrations() >= event.getMaxCapacity()) {
            throw new RuntimeException("Event is full again");
        }
        
        // Register user for event
        Event updatedEvent = eventRegistrationService.registerUserForEvent(event, user);
        
        // Mark as confirmed
        entry.setConfirmed(true);
        waitlistRepository.save(entry);
        
        // Remove from waitlist
        waitlistRepository.delete(entry);
        
        return updatedEvent;
    }
    
    /**
     * Clean expired waitlist entries (run by scheduler)
     */
    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void cleanExpiredWaitlist() {
        LocalDateTime now = LocalDateTime.now();
        List<EventWaitlist> expired = waitlistRepository.findByConfirmedFalseAndExpiresAtBefore(now);
        
        for (EventWaitlist entry : expired) {
            Event event = entry.getEvent();
            waitlistRepository.delete(entry);
            log.info("🗑️ Removed expired waitlist entry: " + entry.getId());
            processWaitlist(event);
        }
    }
}