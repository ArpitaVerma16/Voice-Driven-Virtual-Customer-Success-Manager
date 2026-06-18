package com.vcsm.service;

import com.vcsm.model.Event;
import com.vcsm.repository.EventRepository;
import com.vcsm.repository.EventRegistrationRepository;
import com.vcsm.repository.EventWaitlistRepository;
import com.vcsm.repository.EmailLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private EventWaitlistRepository eventWaitlistRepository;

    @Mock
    private EmailLogRepository emailLogRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void testDeleteEvent_CascadeDeletesAssociatedRecords() {
        Long eventId = 1L;
        Event event = new Event();
        event.setId(eventId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        eventService.deleteEvent(eventId);

        verify(eventWaitlistRepository, times(1)).deleteByEvent(event);
        verify(emailLogRepository, times(1)).deleteByEvent(event);
        verify(eventRegistrationRepository, times(1)).deleteByEvent(event);
        verify(eventRepository, times(1)).delete(event);
    }

    @Test
    void testDeleteEvent_EventNotFound() {
        Long eventId = 2L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        eventService.deleteEvent(eventId);

        verify(eventWaitlistRepository, never()).deleteByEvent(any());
        verify(emailLogRepository, never()).deleteByEvent(any());
        verify(eventRegistrationRepository, never()).deleteByEvent(any());
        verify(eventRepository, never()).delete(any());
    }
}
