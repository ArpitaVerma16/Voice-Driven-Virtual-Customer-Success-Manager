package com.vcsm.security.seed;

import com.vcsm.security.model.AppUser;
import com.vcsm.security.model.UserRole;
import com.vcsm.security.repo.UserRepository;
import com.vcsm.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DefaultAdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.vcsm.repository.UserRepository profileUserRepository;

    @Value("${security.admin.username:}")
    private String adminUsername;

    @Value("${security.admin.password:}")
    private String adminPassword;

    public DefaultAdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, com.vcsm.repository.UserRepository profileUserRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.profileUserRepository = profileUserRepository;
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

        // Auto-create business User profile if it does not exist
        String email = adminUsername.trim();
        if (!profileUserRepository.existsByEmail(email)) {
            User profile = new User(email, "Admin");
            profileUserRepository.save(profile);
        }
    }
}

