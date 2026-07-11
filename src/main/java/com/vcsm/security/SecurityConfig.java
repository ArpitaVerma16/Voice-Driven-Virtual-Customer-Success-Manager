package com.vcsm.security;

import org.springframework.core.env.Environment;
import com.vcsm.security.hmac.HmacAuthenticationFilter;
import com.vcsm.security.jwt.JwtAuthFilter;
import com.vcsm.security.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private Environment environment;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private HmacAuthenticationFilter hmacAuthenticationFilter;
    private UserDetailsServiceImpl userDetailsService;

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**", "/voice-model")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").denyAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/admin/seed").permitAll()
                        .requestMatchers("/api/voice/command").permitAll()
                        .requestMatchers("/api/voice/feedback/**").permitAll()
                        .requestMatchers("/api/chatbot/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        hmacAuthenticationFilter,
                        JwtAuthFilter.class
                )
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        boolean isDevProfile = Arrays.asList(
                environment.getActiveProfiles()
        ).contains("dev");

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> {
                     if (isDevProfile) {
                         auth.requestMatchers("/h2-console/**").permitAll();
                     } else {
                        auth.requestMatchers("/h2-console/**").denyAll();
                     }

                     auth
                              .requestMatchers(
                          "/landing",
                                      "/login",
                                      "/signup",
                                      "/css/**",
                                      "/js/**",
                                      "/images/**",
                                      "/favicon.ico"
                                ).permitAll()
                                .requestMatchers(
                                "/",
                                "/complaints/**",
                                "/events/**",
                                "/analytics/**",
                                "/interaction-history/**",
                                "/voice-analytics/**"
                        ).authenticated()
                        .requestMatchers(
                                "/chatbot/**",
                                "/voice-templates/**"
                        ).authenticated()
                        .requestMatchers(
                                "/profile/**",
                                "/onboarding/**"
                        ).authenticated()
                        .requestMatchers("/audit-logs/**").hasRole("ADMIN")
                        .anyRequest().authenticated();
            })
            .formLogin(form -> form
                    .loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    .permitAll()
            )
            .logout(logout -> logout
                    .logoutSuccessUrl("/landing")
                    .permitAll()
            );

    if (isDevProfile) {
        http.headers(headers ->
                headers.frameOptions(frameOptions ->
                        frameOptions.sameOrigin()
                )
        );
    }

    return http.build();
}

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
