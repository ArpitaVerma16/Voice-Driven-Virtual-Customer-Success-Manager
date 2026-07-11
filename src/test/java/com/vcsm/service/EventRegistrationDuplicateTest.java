package com.vcsm.service;

import com.vcsm.model.Event;
import com.vcsm.model.EventRegistration;
import com.vcsm.model.User;
import com.vcsm.repository.EventRegistrationRepository;
import com.vcsm.repository.EventRepository;
import com.vcsm.repository.UserRepository;
import com.vcsm.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regression tests for duplicate event registrations (issue #482).
 *
 * Two requests racing past the existsByUserAndEvent pre-check must not
 * produce two registration rows or a raw 500: the unique constraint fires
 * on the second insert and the service translates it into the same
 * "already registered" error the pre-check produces.
 */
@ExtendWith(MockitoExtension.class)
class EventRegistrationDuplicateTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private ReminderScheduler reminderScheduler;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private EventService eventService;

    private Event upcomingEvent() {
        Event event = new Event();
        event.setId(1L);
        event.setActive(true);
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setMaxCapacity(100);
        event.setRegistrations(5);
        return event;
    }

    private User resident() {
        User user = new User();
        user.setId(42L);
        return user;
    }

    @Test
    void duplicatePreCheckRejectsSecondRegistration() {
        Event event = upcomingEvent();
        User user = resident();
        when(eventRepository.findWithLockById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(eventRegistrationRepository.existsByUserAndEvent(user, event)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> eventService.registerForEvent(1L, 42L));

        assertEquals("User already registered for this event", ex.getMessage());
        verify(eventRegistrationRepository, never()).save(any());
        assertEquals(5, event.getRegistrations());
    }

    @Test
    void constraintViolationOnRacingInsertIsTranslatedToFriendlyError() {
        // Simulates the second of two concurrent requests: the pre-check saw
        // no registration, but the DB unique constraint fires on insert.
        Event event = upcomingEvent();
        User user = resident();
        when(eventRepository.findWithLockById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(eventRegistrationRepository.existsByUserAndEvent(user, event)).thenReturn(false);
        when(eventRegistrationRepository.saveAndFlush(any(EventRegistration.class)))
                .thenThrow(new DataIntegrityViolationException("unique_user_event"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> eventService.registerForEvent(1L, 42L));

        assertEquals("User already registered for this event", ex.getMessage());
        // The attendance counter must not be inflated by the failed attempt
        assertEquals(5, event.getRegistrations());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void successfulRegistrationIncrementsCountOnce() {
        Event event = upcomingEvent();
        User user = resident();
        EventRegistration saved = new EventRegistration(user, event);
        saved.setId(7L);

        when(eventRepository.findWithLockById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(eventRegistrationRepository.existsByUserAndEvent(user, event)).thenReturn(false);
        when(eventRegistrationRepository.saveAndFlush(any(EventRegistration.class))).thenReturn(saved);
        when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(saved);
        when(jwtService.generateTicketToken(7L, 42L, 1L)).thenReturn("ticket-token");
        when(eventRepository.save(event)).thenReturn(event);

        eventService.registerForEvent(1L, 42L);

        assertEquals(6, event.getRegistrations());
        verify(eventRegistrationRepository, times(1)).saveAndFlush(any(EventRegistration.class));
    }

    @Test
    void registrationUsesPessimisticLockFinder() {
        Event event = upcomingEvent();
        User user = resident();
        when(eventRepository.findWithLockById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(eventRegistrationRepository.existsByUserAndEvent(user, event)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> eventService.registerForEvent(1L, 42L));

        verify(eventRepository).findWithLockById(1L);
        verify(eventRepository, never()).findById(any());
    }
}
