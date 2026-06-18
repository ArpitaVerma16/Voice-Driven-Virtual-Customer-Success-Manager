package com.vcsm.security.service;

import com.vcsm.security.dto.AuthResponse;
import com.vcsm.security.dto.AuthRequest;
import com.vcsm.security.jwt.JwtService;
import com.vcsm.security.model.AppUser;
import com.vcsm.security.model.UserRole;
import com.vcsm.security.repo.UserRepository;
import com.vcsm.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final com.vcsm.repository.UserRepository profileUserRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, com.vcsm.repository.UserRepository profileUserRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.profileUserRepository = profileUserRepository;
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

        // Auto-create business User profile if it does not exist
        if (!profileUserRepository.existsByEmail(req.getUsername())) {
            User profile = new User(req.getUsername(), "Resident");
            profileUserRepository.save(profile);
        }

        return new AuthResponse(jwtService.generateToken(user));
    }

    public AuthResponse login(AppUser user) {
        return new AuthResponse(jwtService.generateToken(user));
    }
}

