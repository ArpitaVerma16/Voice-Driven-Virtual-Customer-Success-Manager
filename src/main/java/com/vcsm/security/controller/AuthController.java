package com.vcsm.security.controller;

import com.vcsm.security.dto.AuthRequest;
import com.vcsm.security.dto.AuthResponse;
import com.vcsm.security.model.AppUser;
import com.vcsm.security.repo.AppUserRepository;
import com.vcsm.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AppUserRepository userRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AppUserRepository userRepository,
            AuthService authService,
            PasswordEncoder passwordEncoder) {
    public AuthController(AppUserRepository userRepository,
                          AuthService authService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody AuthRequest req) {
        return authService.signupResident(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest req) {

        AppUser user = userRepository.findByUsername(req.getUsername()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return authService.login(user);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleBadCred(BadCredentialsException ex) {
        return Map.of(
                "error", ex.getMessage(),
                "success", false
        );
        return errorResponse(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleRuntime(RuntimeException ex) {
        return Map.of(
                "error", ex.getMessage(),
                "success", false
        );
    }
        return errorResponse(ex.getMessage());
    }

    private Map<String, Object> errorResponse(String message) {
        return Map.of(
                "error", message,
                "success", false
        );
    }
}