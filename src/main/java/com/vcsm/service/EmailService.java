package com.vcsm.service;

import com.vcsm.model.EmailLog;
import com.vcsm.model.EmailQueue;
import com.vcsm.model.Event;
import com.vcsm.model.User;
import com.vcsm.repository.EmailLogRepository;
import com.vcsm.repository.EmailQueueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private EmailQueueRepository emailQueueRepository;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    public void sendEventReminder(Event event, User user, String reminderType) {
        String subject = getSubject(reminderType, event.getName());
        String message = buildReminderMessage(event, user, reminderType);
        
        EmailQueue queueItem = new EmailQueue(user, event, user.getEmail(), subject, message);
        emailQueueRepository.save(queueItem);
        
        System.out.println("📨 Queued email reminder to: " + user.getEmail() + " for event: " + event.getName());
    }
    
    /**
     * Send notification when a slot becomes available
     */
    public void sendEventSlotAvailable(Event event, User user) {
        String subject = "🎉 Slot Available for " + event.getName() + "!";
        String message = buildSlotAvailableMessage(event, user);
        
        EmailQueue queueItem = new EmailQueue(user, event, user.getEmail(), subject, message);
        emailQueueRepository.save(queueItem);
        
        System.out.println("📨 Queued slot available email to: " + user.getEmail());
    }

    /**
     * Process an email from the queue, managing retries with exponential backoff
     */
    public void processQueuedEmail(EmailQueue email) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(email.getRecipientEmail());
            helper.setSubject(email.getSubject());
            helper.setText(email.getMessage(), true);
            
            mailSender.send(mimeMessage);
            
            // Log success in audit trail (EmailLog)
            EmailLog log = new EmailLog(email.getUser(), email.getEvent(), email.getRecipientEmail(), email.getSubject(), email.getMessage());
            log.setStatus("SENT");
            emailLogRepository.save(log);
            
            // Update queue item status
            email.setStatus("SENT");
            emailQueueRepository.save(email);
            
            System.out.println("✅ Sent queued email to: " + email.getRecipientEmail());
            
        } catch (Exception e) {
            // Log failure in audit trail (EmailLog)
            EmailLog log = new EmailLog(email.getUser(), email.getEvent(), email.getRecipientEmail(), email.getSubject(), email.getMessage());
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            emailLogRepository.save(log);
            
            // Update queue item attempts and retry state
            int attempts = email.getAttempts() + 1;
            email.setAttempts(attempts);
            email.setErrorMessage(e.getMessage());
            
            if (attempts >= 5) {
                email.setStatus("FAILED");
                System.err.println("❌ Permanently failed to send queued email to " + email.getRecipientEmail() + ": " + e.getMessage());
            } else {
                // Exponential backoff retry delay in minutes: 2^attempts (e.g. 2, 4, 8, 16 mins)
                long backoffMinutes = (long) Math.pow(2, attempts);
                email.setNextAttemptAt(LocalDateTime.now().plusMinutes(backoffMinutes));
                System.out.println("⏳ Failed email to " + email.getRecipientEmail() + ". Scheduling retry in " + backoffMinutes + " mins. Attempt: " + attempts);
            }
            emailQueueRepository.save(email);
        }
    }
    
    private String getSubject(String reminderType, String eventName) {
        switch (reminderType) {
            case "CONFIRMATION":
                return "✅ Registration Confirmed: " + eventName;
            case "DAY_BEFORE":
                return "⏰ Reminder: " + eventName + " is tomorrow!";
            case "HOUR_BEFORE":
                return "🔔 " + eventName + " starts in 1 hour!";
            case "FOLLOW_UP":
                return "📝 How was " + eventName + "?";
            default:
                return "Event Reminder: " + eventName;
        }
    }
    
    private String buildReminderMessage(Event event, User user, String reminderType) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String formattedDate = event.getEventDate() != null ? event.getEventDate().format(formatter) : "TBD";
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>");
        html.append("body { font-family: Arial, sans-serif; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background: linear-gradient(135deg, #8b5cf6, #6d28d9); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center; }");
        html.append(".content { padding: 20px; background: #f8f9fa; border-radius: 0 0 10px 10px; }");
        html.append(".event-details { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; }");
        html.append(".btn { display: inline-block; padding: 10px 20px; background: #8b5cf6; color: white; text-decoration: none; border-radius: 5px; }");
        html.append(".footer { text-align: center; padding: 10px; font-size: 12px; color: #999; }");
        html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<div class='header'><h2>🎉 " + event.getName() + "</h2>");
        html.append("<p>" + getReminderMessage(reminderType) + "</p></div>");
        html.append("<div class='content'>");
        html.append("<div class='event-details'>");
        html.append("<p><strong>📅 Date:</strong> " + formattedDate + "</p>");
        html.append("<p><strong>📍 Location:</strong> " + event.getLocation() + "</p>");
        if (event.getDescription() != null) {
            html.append("<p><strong>📝 Description:</strong> " + event.getDescription() + "</p>");
        }
        html.append("</div>");
        html.append("<p style='text-align: center;'>");
        html.append("<a href='http://localhost:8080/events' class='btn'>View Event Details</a>");
        html.append("</p>");
        html.append("<p>Best regards,<br>VCSM Team</p>");
        html.append("</div>");
        html.append("<div class='footer'><small>This is an automated message. Please do not reply.</small></div>");
        html.append("</div></body></html>");
        
        return html.toString();
    }
    
    private String buildSlotAvailableMessage(Event event, User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String formattedDate = event.getEventDate() != null ? event.getEventDate().format(formatter) : "TBD";
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>");
        html.append("body { font-family: Arial, sans-serif; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background: linear-gradient(135deg, #22c55e, #16a34a); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center; }");
        html.append(".content { padding: 20px; background: #f8f9fa; border-radius: 0 0 10px 10px; }");
        html.append(".event-details { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; }");
        html.append(".btn { display: inline-block; padding: 10px 20px; background: #22c55e; color: white; text-decoration: none; border-radius: 5px; }");
        html.append(".urgent { color: #dc2626; font-weight: bold; }");
        html.append(".footer { text-align: center; padding: 10px; font-size: 12px; color: #999; }");
        html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<div class='header'><h2>🎉 Good News!</h2>");
        html.append("<p>A slot opened up for <strong>" + event.getName() + "</strong></p></div>");
        html.append("<div class='content'>");
        html.append("<p>Hi " + user.getName() + ",</p>");
        html.append("<p>A spot has become available for this event. You have <span class='urgent'>24 hours</span> to confirm your registration.</p>");
        html.append("<div class='event-details'>");
        html.append("<p><strong>📅 Date:</strong> " + formattedDate + "</p>");
        html.append("<p><strong>📍 Location:</strong> " + event.getLocation() + "</p>");
        if (event.getDescription() != null) {
            html.append("<p><strong>📝 Description:</strong> " + event.getDescription() + "</p>");
        }
        html.append("</div>");
        html.append("<p style='text-align: center;'>");
        html.append("<a href='http://localhost:8080/api/events/waitlist/confirm?eventId=" + event.getId() + "&userId=" + user.getId() + "' class='btn'>✅ Confirm Registration</a>");
        html.append("</p>");
        html.append("<p style='color: #666; font-size: 14px;'>This link will expire in <strong>24 hours</strong>.</p>");
        html.append("<p>Best regards,<br>VCSM Team</p>");
        html.append("</div>");
        html.append("<div class='footer'><small>This is an automated message. Please do not reply.</small></div>");
        html.append("</div></body></html>");
        
        return html.toString();
    }
    
    private String getReminderMessage(String reminderType) {
        switch (reminderType) {
            case "CONFIRMATION": return "You have successfully registered!";
            case "DAY_BEFORE": return "This event is happening tomorrow!";
            case "HOUR_BEFORE": return "This event starts in 1 hour!";
            case "FOLLOW_UP": return "We hope you enjoyed the event!";
            default: return "Event Reminder";
        }
    }
}