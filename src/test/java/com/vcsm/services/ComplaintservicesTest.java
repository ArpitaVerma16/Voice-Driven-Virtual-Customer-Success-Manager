package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private ComplaintService complaintService;

    private Complaint testComplaint;

    @BeforeEach
void setUp() {
        testComplaint = new Complaint();
        testComplaint.setId(1L);
        testComplaint.setResidentName("Test User");
        testComplaint.setDescription("Test complaint description");
        testComplaint.setCategory("NOISE");
        testComplaint.setStatus(Complaint.ComplaintStatus.OPEN);
        testComplaint.setCreatedAt(LocalDateTime.now());
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
        when(complaintRepository.findAll()).thenReturn(complaints);

        List<Complaint> result = complaintService.getAllComplaints();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(complaintRepository, times(1)).findAll();
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
        when(complaintRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Complaint> result = complaintService.getComplaintById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateComplaintStatus() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(testComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(testComplaint);

        Complaint updated = complaintService.updateComplaintStatus(1L, Complaint.ComplaintStatus.IN_PROGRESS);

        assertNotNull(updated);
        assertEquals(Complaint.ComplaintStatus.IN_PROGRESS, updated.getStatus());
        verify(complaintRepository, times(1)).save(any(Complaint.class));
    }

    @Test
    void testDeleteComplaint() {
        doNothing().when(complaintRepository).deleteById(1L);
        
        complaintService.deleteComplaint(1L);
        
        verify(complaintRepository, times(1)).deleteById(1L);
    }
}