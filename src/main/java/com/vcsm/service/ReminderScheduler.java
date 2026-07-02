package com.vcsm.service;$1

import com.vcsm.config.AppConstants;

import com.vcsm.model.EmailQueue;
import com.vcsm.model.Event;
import com.vcsm.model.User;
import com.vcsm.repository.EmailQueueRepository;
import com.vcsm.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReminderScheduler {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    @org.springframework.context.annotation.Lazy
    private EventRegistrationService eventRegistrationService;

    @Autowired
    private EmailQueueRepository emailQueueRepository;
    
    /**
     * Runs every hour to check for events
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void sendEventReminders() {
        System.out.println("⏰ Checking for event reminders...");
        
        LocalDateTime now = LocalDateTime.now();
        List<Event> upcomingEvents = eventRepository.findByEventDateAfter(now);
        
        for (Event event : upcomingEvents) {
            LocalDateTime eventDate = event.getEventDate();
            long hoursUntilEvent = ChronoUnit.HOURS.between(now, eventDate);
            long daysUntilEvent = ChronoUnit.DAYS.between(now, eventDate);
            
            // Check if reminder needed
            List<User> registrants = eventRegistrationService.getEventRegistrants(event);
            
            for (User user : registrants) {
                if (!user.isEmailNotifications()) continue; // Skip if notifications disabled
                
                // 24-hour reminder
                if (daysUntilEvent == 1 && hoursUntilEvent >= 23 && hoursUntilEvent < 24) {
                    emailService.sendEventReminder(event, user, "DAY_BEFORE");
                }
                // 1-hour reminder
                else if (hoursUntilEvent == 1) {
                    emailService.sendEventReminder(event, user, "HOUR_BEFORE");
                }
            }
        }
    }
    
    /**
     * Send confirmation email immediately on registration
     */
    public void sendRegistrationConfirmation(Event event, User user) {
        if (user.isEmailNotifications()) {
            emailService.sendEventReminder(event, user, "CONFIRMATION");
        }
    }

    /**
     * Polls the email queue and processes pending emails that are due
     */
    @Scheduled(fixedDelay = AppConstants.SCHEDULER_POLL_MS) // Every 10 seconds
    public void processEmailQueue() {
        LocalDateTime now = LocalDateTime.now();
        List<EmailQueue> pendingEmails = emailQueueRepository.findByStatusAndNextAttemptAtBefore("PENDING", now);
        for (EmailQueue email : pendingEmails) {
            emailService.processQueuedEmail(email);
        }
    }
}
