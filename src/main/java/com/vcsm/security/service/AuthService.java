package com.vcsm.security.service;

import com.vcsm.security.dto.AuthResponse;
import com.vcsm.security.dto.AuthRequest;
import com.vcsm.security.jwt.JwtService;
import com.vcsm.security.model.AppUser;
import com.vcsm.security.model.UserRole;
import com.vcsm.security.repo.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse signupResident(AuthRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRoles(Set.of(UserRole.ROLE_RESIDENT));
        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user));
    }

    public AuthResponse login(AppUser user) {
        return new AuthResponse(jwtService.generateToken(user));
    }
}

