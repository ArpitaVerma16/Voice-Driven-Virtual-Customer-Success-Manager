package com.vcsm.security.service;

import com.vcsm.security.dto.AuthResponse;
import com.vcsm.security.dto.AuthRequest;
import com.vcsm.security.jwt.JwtService;
import com.vcsm.security.model.AppUser;
import com.vcsm.security.model.UserRole;
import com.vcsm.security.repo.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final com.vcsm.repository.UserRepository profileUserRepository;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
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

        String token = jwtService.generateToken(user);
        // ✅ AuthResponse(token, refreshToken, email, role)
        String role = user.getRoles().stream()
            .findFirst()
            .map(Enum::name)
            .orElse("ROLE_RESIDENT");
        return new AuthResponse(token, null, user.getUsername(), role);
    }

    public AuthResponse login(AppUser user) {
        String token = jwtService.generateToken(user);
        // ✅ AuthResponse(token, refreshToken, email, role)
        String role = user.getRoles().stream()
            .findFirst()
            .map(Enum::name)
            .orElse("UNKNOWN");
        return new AuthResponse(token, null, user.getUsername(), role);
    }
}