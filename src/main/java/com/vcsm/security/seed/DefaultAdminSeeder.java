package com.vcsm.security.seed;

import com.vcsm.security.model.AppUser;
import com.vcsm.security.model.UserRole;
import com.vcsm.security.repo.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DefaultAdminSeeder implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.admin.username:}")
    private String adminUsername;

    @Value("${security.admin.password:}")
    private String adminPassword;

    public DefaultAdminSeeder(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminUsername == null || adminUsername.isBlank()) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            // safer default: do not create admin if password missing
            return;
        }

        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }

        AppUser admin = new AppUser();
        admin.setUsername(adminUsername.trim());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRoles(Set.of(UserRole.ROLE_ADMIN));
        userRepository.save(admin);
    }
}

