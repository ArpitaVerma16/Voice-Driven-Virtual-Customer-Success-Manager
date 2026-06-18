package com.vcsm.security.service;

import com.vcsm.security.repo.AppUserRepository;
import com.vcsm.security.model.AppUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final com.vcsm.repository.UserRepository profileUserRepository;

    public CustomUserDetailsService(AppUserRepository userRepository, com.vcsm.repository.UserRepository profileUserRepository) {
        this.userRepository = userRepository;
        this.profileUserRepository = profileUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Long profileId = profileUserRepository.findByEmail(username)
                .map(com.vcsm.model.User::getId)
                .orElse(user.getId());

        return new CustomUserDetails(
                profileId,
                user.getUsername(),
                user.getPasswordHash(),
                user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority(r.name()))
                        .collect(Collectors.toSet())
        );
    }
}

