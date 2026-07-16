package com.vcsm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/h2-console/**").denyAll()
                .antMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
            .logout()
                .permitAll()
                .and()
            .csrf()
                .ignoringAntMatchers("/api/**")
                .and()
            .headers()
                .frameOptions().disable();

        return http.build();
    }
    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authz -> authz
            // GraphQL endpoints
            .requestMatchers("/graphql", "/graphiql").permitAll()
            .requestMatchers("/graphql/**").permitAll()
            // Rest of your existing configuration...
        )
        // Rest of your security config...
    return http.build();
}
}
