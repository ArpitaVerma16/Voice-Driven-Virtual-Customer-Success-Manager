package com.vcsm.security.service;

import com.vcsm.security.dto.AuthRequest;
import com.vcsm.security.dto.AuthResponse;
import com.vcsm.security.model.AppUser;
import com.vcsm.security.repo.UserRepository;
import com.vcsm.model.User;
import com.vcsm.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.vcsm.repository.UserRepository profileUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void testSignupResident_CreatesAppUserAndProfileUser() {
        AuthRequest req = new AuthRequest();
        req.setUsername("resident@example.com");
        req.setPassword("password123");

        when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("encodedPassword");
        when(profileUserRepository.existsByEmail(req.getUsername())).thenReturn(false);
        when(jwtService.generateToken(any(AppUser.class))).thenReturn("jwt-token");

        AuthResponse resp = authService.signupResident(req);

        assertNotNull(resp);
        verify(userRepository, times(1)).save(any(AppUser.class));
        verify(profileUserRepository, times(1)).save(any(User.class));
    }
}
