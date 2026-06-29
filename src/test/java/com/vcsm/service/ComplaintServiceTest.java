package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private PriorityClassifierService priorityClassifierService;

    @Mock
    private UserActivityService userActivityService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ComplaintService complaintService;

    private Complaint testComplaint;

    @BeforeEach
    void setUp() {
        testComplaint = new Complaint();
        testComplaint.setId(1L);
        testComplaint.setResidentName("Test User");
        testComplaint.setDescription("Test complaint description");
        testComplaint.setCategory(Complaint.ComplaintCategory.NOISE);
        testComplaint.setStatus(Complaint.ComplaintStatus.OPEN);
        testComplaint.setCreatedAt(LocalDateTime.now());

        // Mock SecurityContext to return an authenticated user with ROLE_ADMIN
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(auth.getName()).thenReturn("admin@example.com");
        
        org.springframework.security.core.authority.SimpleGrantedAuthority authority = 
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN");
        lenient().when(auth.getAuthorities()).thenAnswer(invocation -> Collections.singletonList(authority));

        org.springframework.security.core.context.SecurityContext context = mock(org.springframework.security.core.context.SecurityContext.class);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);

        // Stub dependency interactions to prevent NPEs and unauthorized errors
        lenient().when(priorityClassifierService.classifyPriority(any(), any())).thenReturn("MEDIUM");
        lenient().when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User("admin@example.com", "Admin", "password")));
    }

    @Test
    void testFileComplaint_Success() {
        when(complaintRepository.save(any(Complaint.class))).thenReturn(testComplaint);

        Complaint saved = complaintService.fileComplaint(testComplaint);

        assertNotNull(saved);
        assertEquals("Test User", saved.getResidentName());
        verify(complaintRepository, times(1)).save(any(Complaint.class));
    }

    @Test
    void testGetAllComplaints() {
        List<Complaint> complaints = Arrays.asList(testComplaint);
        when(complaintRepository.findAllOrderByCreatedAtDesc()).thenReturn(complaints);

        List<Complaint> result = complaintService.getAllComplaints();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(complaintRepository, times(1)).findAllOrderByCreatedAtDesc();
    }

    @Test
    void testGetComplaintById_Found() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(testComplaint));

        Optional<Complaint> result = complaintService.getComplaintById(1L);

        assertTrue(result.isPresent());
        assertEquals("Test User", result.get().getResidentName());
    }

    @Test
    void testGetComplaintById_NotFound() {
        lenient().when(complaintRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Complaint> result = complaintService.getComplaintById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateComplaintStatus() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(testComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(testComplaint);

        Complaint updated = complaintService.updateStatus(1L, "IN_PROGRESS", "admin", "Updating status");

        assertNotNull(updated);
        assertEquals(Complaint.ComplaintStatus.IN_PROGRESS, updated.getStatus());
        verify(complaintRepository, times(1)).save(any(Complaint.class));
    }

    @Test
    void testDeleteComplaint() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(testComplaint));
        doNothing().when(complaintRepository).deleteById(1L);
        
        complaintService.deleteComplaint(1L);
        
        verify(complaintRepository, times(1)).deleteById(1L);
    }
}