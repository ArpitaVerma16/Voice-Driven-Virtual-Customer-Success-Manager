package com.vcsm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
/*
@Autowired
private RateLimitFilter rateLimitFilter;

// In configure method
http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
*/
/*
@Configuration
@EnableWebSecurity
*/
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").denyAll()
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout
                .permitAll()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
