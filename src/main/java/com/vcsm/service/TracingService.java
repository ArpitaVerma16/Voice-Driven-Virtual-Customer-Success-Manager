package com.vcsm.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TracingService {

    @Autowired
    private Tracer tracer;

    /**
     * Create a trace span for an operation
     */
    public Span createSpan(String spanName) {
        return tracer.spanBuilder(spanName).startSpan();
    }

    /**
     * End a span and set attributes
     */
    public void endSpan(Span span, String operation, String status) {
        if (span != null) {
            span.setAttribute("operation", operation);
            span.setAttribute("status", status);
            span.setAttribute("timestamp", System.currentTimeMillis());
            span.end();
        }
    }

    /**
     * Set error on span
     */
    public void setError(Span span, Throwable error) {
        if (span != null) {
            span.setStatus(StatusCode.ERROR, error.getMessage());
            span.setAttribute("error.type", error.getClass().getSimpleName());
        }
    }

    /**
     * Add custom attributes to span
     */
    public void addAttributes(Span span, Map<String, String> attributes) {
        if (span != null && attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                span.setAttribute(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Create a span and auto-close it
     */
    public AutoCloseableSpan createAutoSpan(String spanName) {
        Span span = createSpan(spanName);
        return new AutoCloseableSpan(span);
    }

    public static class AutoCloseableSpan implements AutoCloseable {
        private final Span span;

        public AutoCloseableSpan(Span span) {
            this.span = span;
        }

        public Span getSpan() { return span; }

        @Override
        public void close() {
            if (span != null) {
                span.end();
            }
        }

        public void setAttribute(String key, String value) {
            if (span != null) {
                span.setAttribute(key, value);
            }
        }
    }
}