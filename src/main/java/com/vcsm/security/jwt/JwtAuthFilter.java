package com.vcsm.security.jwt;

import com.vcsm.security.service.CustomUserDetailsService;
import com.vcsm.security.model.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring("Bearer ".length());

        if (token == null || token.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication token");
            return;
        }

        if (!jwtService.isTokenValid(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        String username = jwtService.extractUsername(token);
        if (username == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token claims");
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Roles from token (still map to authorities)
        String rolesCsv = jwtService.extractRolesCsv(token);
        List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesCsv.split(","))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());


        // Fallback to userDetails roles if rolesCsv empty
        if (authorities.isEmpty()) {
            authorities = userDetails.getAuthorities().stream()
                    .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                    .collect(Collectors.toList());
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}

