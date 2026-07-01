package com.vcsm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vcsm.model.Event;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventService eventService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventController eventController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
    }

    @Test
    void testCreateEvent_Valid() throws Exception {
        Event event = new Event();
        event.setName("Annual Meet");
        event.setEventDate(LocalDateTime.now().plusDays(2));
        event.setMaxCapacity(50);

        when(eventService.createEvent(any(Event.class))).thenReturn(event);

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateEvent_InvalidCapacity() throws Exception {
        Event event = new Event();
        event.setName("Annual Meet");
        event.setEventDate(LocalDateTime.now().plusDays(2));
        event.setMaxCapacity(0); // Invalid capacity: must be >= 1

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateEvent_PastDate() throws Exception {
        Event event = new Event();
        event.setName("Annual Meet");
        event.setEventDate(LocalDateTime.now().minusDays(1)); // Invalid date: past
        event.setMaxCapacity(50);

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateEvent_Valid() throws Exception {
        Event event = new Event();
        event.setName("Annual Meet");
        event.setEventDate(LocalDateTime.now().plusDays(2));
        event.setMaxCapacity(50);

        when(eventService.updateEvent(eq(1L), any(Event.class))).thenReturn(event);

        mockMvc.perform(put("/api/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateEvent_InvalidCapacity() throws Exception {
        Event event = new Event();
        event.setName("Annual Meet");
        event.setEventDate(LocalDateTime.now().plusDays(2));
        event.setMaxCapacity(-5); // Invalid capacity: must be >= 1

        mockMvc.perform(put("/api/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateEvent_PastDate() throws Exception {
        Event event = new Event();
        event.setName("Annual Meet");
        event.setEventDate(LocalDateTime.now().minusDays(2)); // Invalid date: past
        event.setMaxCapacity(50);

        mockMvc.perform(put("/api/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }
}
