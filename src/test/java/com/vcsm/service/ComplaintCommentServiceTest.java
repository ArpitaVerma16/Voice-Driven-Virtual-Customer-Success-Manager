package com.vcsm.service;

import com.vcsm.dto.ComplaintCommentDTO;
import com.vcsm.model.Complaint;
import com.vcsm.model.ComplaintComment;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintCommentRepository;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintCommentServiceTest {

    @Mock
    private ComplaintCommentRepository complaintCommentRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ComplaintCommentService complaintCommentService;

    private User resident;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        resident = new User();
        resident.setId(1L);
        resident.setEmail("resident@example.com");
        resident.setName("Test Resident");

        complaint = new Complaint();
        complaint.setId(100L);
        complaint.setUser(resident);
    }

    private void mockAuthentication(String username, String role) {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn(username);
        
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role));
        doReturn(authorities).when(authentication).getAuthorities();
    }

    @Test
    void addComment_Success_AsResident() {
        mockAuthentication("resident@example.com", "ROLE_USER");
        
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(userRepository.findByEmail("resident@example.com")).thenReturn(Optional.of(resident));
        
        ComplaintComment savedComment = new ComplaintComment(complaint, resident, "This is a comment");
        savedComment.setId(1L);
        when(complaintCommentRepository.save(any(ComplaintComment.class))).thenReturn(savedComment);

        ComplaintCommentDTO result = complaintCommentService.addComment(100L, "This is a comment");

        assertNotNull(result);
        assertEquals("This is a comment", result.getContent());
        assertFalse(result.isAdmin());
        verify(notificationService).sendGlobalNotification(any());
    }

    @Test
    void addComment_Failure_BlankContent() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> complaintCommentService.addComment(100L, "   "));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("cannot be empty"));
    }

    @Test
    void addComment_Failure_UnauthorizedAccess() {
        mockAuthentication("other@example.com", "ROLE_USER");
        
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> complaintCommentService.addComment(100L, "Valid comment"));
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void getComments_Success_AsAdmin() {
        mockAuthentication("admin@example.com", "ROLE_ADMIN");
        
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(complaintCommentRepository.findByComplaintIdOrderByCreatedAtAsc(100L))
            .thenReturn(Collections.singletonList(new ComplaintComment(complaint, resident, "Admin viewing comment")));

        List<ComplaintCommentDTO> results = complaintCommentService.getCommentsForComplaint(100L);

        assertEquals(1, results.size());
    }
}
