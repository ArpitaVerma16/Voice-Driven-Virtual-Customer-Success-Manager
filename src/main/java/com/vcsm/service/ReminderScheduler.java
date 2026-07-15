package com.vcsm.service;$1

import com.vcsm.config.AppConstants;

import com.vcsm.model.EmailQueue;
import com.vcsm.model.Event;
import com.vcsm.model.ReminderQueue;
import com.vcsm.model.User;
import com.vcsm.repository.EmailQueueRepository;
import com.vcsm.repository.EventRepository;
import com.vcsm.repository.ReminderQueueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@lombok.RequiredArgsConstructor
public class ReminderScheduler {

    private final EventRepository eventRepository;

    private final EmailService emailService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private EventRegistrationService eventRegistrationService;

    private final EmailQueueRepository emailQueueRepository;

    private final ReminderQueueRepository reminderQueueRepository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReminderScheduler.class);

    /**
     * Queue reminder entries when a user registers for an event.
     */
    public void queueRegistrationReminders(Event event, User user) {
        if (!user.isEmailNotifications()) return;

        LocalDateTime eventDate = event.getEventDate();

        // Queue 24-hour reminder
        ReminderQueue dayBefore = new ReminderQueue(event, user, "DAY_BEFORE", eventDate.minusDays(1));
        reminderQueueRepository.save(dayBefore);

        // Queue 1-hour reminder
        ReminderQueue hourBefore = new ReminderQueue(event, user, "HOUR_BEFORE", eventDate.minusHours(1));
        reminderQueueRepository.save(hourBefore);

        log.info("📅 Queued reminders for user {} on event {}", user.getEmail(), event.getName());
    }

    /**
     * Process due reminder queue entries every 15 minutes.
     */
    @Scheduled(fixedDelay = 900000)
    public void processReminderQueue() {
        LocalDateTime now = LocalDateTime.now();
        List<ReminderQueue> dueReminders = reminderQueueRepository.findByStatusAndScheduledAtBefore("PENDING", now);

        for (ReminderQueue reminder : dueReminders) {
            try {
                emailService.sendEventReminder(reminder.getEvent(), reminder.getUser(), reminder.getReminderType());
                reminder.setStatus("SENT");
                reminder.setSentAt(now);
                reminderQueueRepository.save(reminder);
                log.info("✅ Processed reminder {} for user {} on event {}",
                    reminder.getReminderType(), reminder.getUser().getEmail(), reminder.getEvent().getName());
            } catch (Exception e) {
                reminder.setStatus("FAILED");
                reminderQueueRepository.save(reminder);
                log.error("Failed to process reminder {} for user {}: {}",
                    reminder.getReminderType(), reminder.getUser().getEmail(), e.getMessage(), e);
            }
        }
    }

    /**
     * Runs every hour to check for events (legacy fallback, ensures no missed reminders)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void sendEventReminders() {
        log.info("⏰ Checking for event reminders (legacy fallback)...");

        LocalDateTime now = LocalDateTime.now();
        List<Event> upcomingEvents = eventRepository.findByEventDateAfter(now);

        for (Event event : upcomingEvents) {
            LocalDateTime eventDate = event.getEventDate();
            long hoursUntilEvent = ChronoUnit.HOURS.between(now, eventDate);
            long daysUntilEvent = ChronoUnit.DAYS.between(now, eventDate);

            List<User> registrants = eventRegistrationService.getEventRegistrants(event);

            for (User user : registrants) {
                if (!user.isEmailNotifications()) continue;

                String reminderType = null;
                if (daysUntilEvent == 1 && hoursUntilEvent >= 23 && hoursUntilEvent < 24) {
                    reminderType = "DAY_BEFORE";
                } else if (hoursUntilEvent == 1) {
                    reminderType = "HOUR_BEFORE";
                }

                if (reminderType != null) {
                    boolean alreadyQueued = !reminderQueueRepository
                        .findByEventIdAndUserIdAndReminderType(event.getId(), user.getId(), reminderType)
                        .isEmpty();
                    if (!alreadyQueued) {
                        emailService.sendEventReminder(event, user, reminderType);
                        ReminderQueue rq = new ReminderQueue(event, user, reminderType, now);
                        rq.setStatus("SENT");
                        rq.setSentAt(now);
                        reminderQueueRepository.save(rq);
                        log.info("✅ Fallback reminder {} sent to user {} for event {}",
                            reminderType, user.getEmail(), event.getName());
                    }
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
    @Scheduled(fixedDelay = 10000)
    public void processEmailQueue() {
        LocalDateTime now = LocalDateTime.now();
        List<EmailQueue> pendingEmails = emailQueueRepository.findByStatusAndNextAttemptAtBefore("PENDING", now);
        for (EmailQueue email : pendingEmails) {
            emailService.processQueuedEmail(email);
        }
    }
}