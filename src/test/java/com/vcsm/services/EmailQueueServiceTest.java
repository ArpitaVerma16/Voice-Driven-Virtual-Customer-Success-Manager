package com.vcsm.services;

import com.vcsm.model.EmailLog;
import com.vcsm.model.EmailQueue;
import com.vcsm.model.Event;
import com.vcsm.model.User;
import com.vcsm.repository.EmailLogRepository;
import com.vcsm.repository.EmailQueueRepository;
import com.vcsm.service.EmailService;
import com.vcsm.service.ReminderScheduler;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailQueueServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private EmailQueueRepository emailQueueRepository;

    @InjectMocks
    private EmailService emailService;

    @InjectMocks
    private ReminderScheduler reminderScheduler;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    public void setUp() {
        testUser = new User("resident@example.com", "Test User", "password");
        testUser.setEmailNotifications(true);

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Sample Event");
        testEvent.setEventDate(LocalDateTime.now().plusDays(2));
        testEvent.setLocation("Community Hall");

        // Inject dependencies manually where needed because InjectMocks might not inject cross-dependencies
        ReflectionTestUtils.setField(emailService, "fromEmail", "vcsm@example.com");
        ReflectionTestUtils.setField(reminderScheduler, "emailService", emailService);
        ReflectionTestUtils.setField(reminderScheduler, "emailQueueRepository", emailQueueRepository);
    }

    @Test
    public void testSendEventReminder_QueuesEmail() {
        emailService.sendEventReminder(testEvent, testUser, "CONFIRMATION");

        verify(emailQueueRepository, times(1)).save(any(EmailQueue.class));
    }

    @Test
    public void testProcessEmailQueue_SendsPendingEmails() {
        EmailQueue pendingEmail = new EmailQueue(testUser, testEvent, "resident@example.com", "Subject", "Message");
        pendingEmail.setId(1L);

        when(emailQueueRepository.findByStatusAndNextAttemptAtBefore(eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(pendingEmail));

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);

        reminderScheduler.processEmailQueue();

        verify(mailSender, times(1)).send(mockMimeMessage);
        verify(emailLogRepository, times(1)).save(any(EmailLog.class));
        verify(emailQueueRepository, times(1)).save(argThat(email -> "SENT".equals(email.getStatus())));
    }

    @Test
    public void testProcessEmailQueue_RetriesOnFailure() {
        EmailQueue pendingEmail = new EmailQueue(testUser, testEvent, "resident@example.com", "Subject", "Message");
        pendingEmail.setId(1L);
        pendingEmail.setAttempts(1);

        when(emailQueueRepository.findByStatusAndNextAttemptAtBefore(eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(pendingEmail));

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        doThrow(new RuntimeException("Mail server down")).when(mailSender).send(any(MimeMessage.class));

        reminderScheduler.processEmailQueue();

        verify(emailLogRepository, times(1)).save(any(EmailLog.class));
        verify(emailQueueRepository, times(1)).save(argThat(email -> 
                "PENDING".equals(email.getStatus()) && 
                email.getAttempts() == 2 && 
                email.getNextAttemptAt().isAfter(LocalDateTime.now().plusMinutes(3))
        ));
    }

    @Test
    public void testProcessEmailQueue_FailsPermanentlyAfterMaxAttempts() {
        EmailQueue pendingEmail = new EmailQueue(testUser, testEvent, "resident@example.com", "Subject", "Message");
        pendingEmail.setId(1L);
        pendingEmail.setAttempts(4); // Next failure will make it 5

        when(emailQueueRepository.findByStatusAndNextAttemptAtBefore(eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(pendingEmail));

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        doThrow(new RuntimeException("Mail server down")).when(mailSender).send(any(MimeMessage.class));

        reminderScheduler.processEmailQueue();

        verify(emailLogRepository, times(1)).save(any(EmailLog.class));
        verify(emailQueueRepository, times(1)).save(argThat(email -> 
                "FAILED".equals(email.getStatus()) && 
                email.getAttempts() == 5
        ));
    }
}
