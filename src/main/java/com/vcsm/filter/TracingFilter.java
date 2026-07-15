package com.vcsm.filter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TracingFilter extends OncePerRequestFilter {

    @Autowired
    private Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip health checks and static resources
        if (path.startsWith("/actuator") || path.startsWith("/static") || path.startsWith("/css")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Create span for incoming request
        Span span = tracer.spanBuilder("HTTP " + request.getMethod() + " " + path)
            .setAttribute("http.method", request.getMethod())
            .setAttribute("http.url", request.getRequestURL().toString())
            .setAttribute("http.path", path)
            .setAttribute("http.client.ip", request.getRemoteAddr())
            .startSpan();

        try {
            // Set span in context
            io.opentelemetry.context.Context context = io.opentelemetry.context.Context.current()
                .with(span);
            io.opentelemetry.context.Scope scope = context.makeCurrent();

            try {
                // Process request
                long startTime = System.currentTimeMillis();
                filterChain.doFilter(request, response);
                long duration = System.currentTimeMillis() - startTime;

                // Set response attributes
                span.setAttribute("http.status", response.getStatus());
                span.setAttribute("duration.ms", duration);
                span.setAttribute("http.success", response.getStatus() < 400);

            } finally {
                scope.close();
                span.end();
            }

        } catch (Exception e) {
            span.setAttribute("error", e.getMessage());
            span.setAttribute("error.type", e.getClass().getSimpleName());
            span.setAttribute("http.success", false);
            span.end();
            throw e;
        }
    }
}