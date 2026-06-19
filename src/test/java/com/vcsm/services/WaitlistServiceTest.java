package com.vcsm.services;

import com.vcsm.model.Event;
import com.vcsm.model.EventWaitlist;
import com.vcsm.model.User;
import com.vcsm.repository.EventWaitlistRepository;
import com.vcsm.service.EmailService;
import com.vcsm.service.WaitlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WaitlistServiceTest {

    @Mock
    private EventWaitlistRepository waitlistRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private WaitlistService waitlistService;

    private User testUser1;
    private User testUser2;
    private Event testEvent;

    @BeforeEach
    public void setUp() {
        testUser1 = new User("resident1@example.com", "User One", "password");
        testUser1.setId(1L);

        testUser2 = new User("resident2@example.com", "User Two", "password");
        testUser2.setId(2L);

        testEvent = new Event();
        testEvent.setId(10L);
        testEvent.setName("Sample Event");
        testEvent.setMaxCapacity(10);
        testEvent.setRegistrations(9); // 1 slot free
    }

    @Test
    public void testCleanExpiredWaitlist_DeletesExpiredAndPromotesNext() {
        EventWaitlist expiredEntry = new EventWaitlist(testEvent, testUser1);
        expiredEntry.setId(100L);
        expiredEntry.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        expiredEntry.setConfirmed(false);

        EventWaitlist nextEntry = new EventWaitlist(testEvent, testUser2);
        nextEntry.setId(101L);
        nextEntry.setConfirmed(false);

        // When cleanExpiredWaitlist queries for expired entries
        when(waitlistRepository.findByConfirmedFalseAndExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(expiredEntry));

        // When processWaitlist queries for the next waitlisted user
        when(waitlistRepository.findFirstByEventAndConfirmedFalseOrderByJoinedAtAsc(testEvent))
                .thenReturn(Optional.of(nextEntry));

        waitlistService.cleanExpiredWaitlist();

        // Verify expired entry is deleted
        verify(waitlistRepository, times(1)).delete(expiredEntry);

        // Verify next waitlist entry is promoted (notified and expiresAt updated)
        verify(emailService, times(1)).sendEventSlotAvailable(testEvent, testUser2);
        verify(waitlistRepository, times(1)).save(nextEntry);
    }
}
