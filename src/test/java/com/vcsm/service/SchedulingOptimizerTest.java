package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.VenueReservation;
import com.vcsm.repository.VenueReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingOptimizerTest {

    @Mock
    private VenueReservationRepository venueReservationRepository;

    @InjectMocks
    private SchedulingOptimizer schedulingOptimizer;

    private User testUser;
    private List<VenueReservation> existingReservations;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Alice");

        existingReservations = new ArrayList<>();
        LocalDateTime tomorrow2PM = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow4PM = tomorrow2PM.plusHours(2);
        VenueReservation res = new VenueReservation("Clubhouse", testUser, tomorrow2PM, tomorrow4PM);
        existingReservations.add(res);

        lenient().when(venueReservationRepository.findByVenueNameIgnoreCaseAndStatus("Clubhouse", "CONFIRMED"))
                .thenReturn(existingReservations);
    }

    @Test
    void testHasConflict_True() {
        LocalDateTime tomorrow3PM = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow4PM = tomorrow3PM.plusHours(1);

        boolean conflict = schedulingOptimizer.hasConflict("Clubhouse", tomorrow3PM, tomorrow4PM);
        assertTrue(conflict);
    }

    @Test
    void testHasConflict_False() {
        LocalDateTime tomorrow4PM = LocalDateTime.now().plusDays(1).withHour(16).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow5PM = tomorrow4PM.plusHours(1);

        boolean conflict = schedulingOptimizer.hasConflict("Clubhouse", tomorrow4PM, tomorrow5PM);
        assertFalse(conflict);
    }

    @Test
    void testFindAlternatives() {
        LocalDateTime requestedStart = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime requestedEnd = requestedStart.plusHours(2);

        List<LocalDateTime[]> alternatives = schedulingOptimizer.findAlternatives("Clubhouse", requestedStart, requestedEnd);

        assertNotNull(alternatives);
        assertFalse(alternatives.isEmpty());

        LocalDateTime beforeStart = alternatives.stream()
                .filter(slot -> slot[0].isBefore(requestedStart))
                .map(slot -> slot[0])
                .findFirst()
                .orElse(null);

        LocalDateTime afterStart = alternatives.stream()
                .filter(slot -> slot[0].isAfter(requestedStart) || slot[0].isEqual(requestedEnd))
                .map(slot -> slot[0])
                .findFirst()
                .orElse(null);

        assertNotNull(beforeStart);
        assertNotNull(afterStart);
        
        assertEquals(requestedStart.minusHours(2), beforeStart);
        assertEquals(requestedEnd, afterStart);
    }

    @Test
    void testBookVenue_Success() {
        LocalDateTime tomorrow4PM = LocalDateTime.now().plusDays(1).withHour(16).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow5PM = tomorrow4PM.plusHours(1);

        when(venueReservationRepository.save(any(VenueReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VenueReservation res = schedulingOptimizer.bookVenue("Clubhouse", testUser, tomorrow4PM, tomorrow5PM);

        assertNotNull(res);
        assertEquals("Clubhouse", res.getVenueName());
        assertEquals(testUser, res.getUser());
        assertEquals(tomorrow4PM, res.getStartTime());
        assertEquals(tomorrow5PM, res.getEndTime());
        verify(venueReservationRepository, times(1)).save(any(VenueReservation.class));
    }

    @Test
    void testBookVenue_ConflictException() {
        LocalDateTime tomorrow3PM = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow4PM = tomorrow3PM.plusHours(1);

        assertThrows(RuntimeException.class, () -> {
            schedulingOptimizer.bookVenue("Clubhouse", testUser, tomorrow3PM, tomorrow4PM);
        });
        verify(venueReservationRepository, never()).save(any(VenueReservation.class));
    }
}
