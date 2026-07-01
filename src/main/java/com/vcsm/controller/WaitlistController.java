package com.vcsm.controller;

import com.vcsm.model.Event;
import com.vcsm.model.User;

import com.vcsm.repository.EventRepository;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.WaitlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.vcsm.security.service.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/events/waitlist")
public class WaitlistController {

    @Autowired
    private WaitlistService waitlistService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/{eventId}/join")
    public ResponseEntity<?> joinWaitlist(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Security fix:
        // Never trust a client-supplied user ID.
        // Always use the authenticated user's identity from the security context.
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        Long userId = userDetails.getId();
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (eventOpt.isEmpty())
            return ResponseEntity.notFound().build();
        if (userOpt.isEmpty())
            return ResponseEntity.notFound().build();

        try {
            waitlistService.joinWaitlist(eventOpt.get(), userOpt.get());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Added to waitlist");
            response.put("position", waitlistService.getWaitlistPosition(eventOpt.get(), userOpt.get()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{eventId}/leave")
    public ResponseEntity<?> leaveWaitlist(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Security fix:
        // Use the authenticated user's identity instead of accepting
        // a userId from the request.
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        Long userId = userDetails.getId();

        Optional<Event> eventOpt = eventRepository.findById(eventId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (eventOpt.isEmpty())
            return ResponseEntity.notFound().build();
        if (userOpt.isEmpty())
            return ResponseEntity.notFound().build();

        waitlistService.leaveWaitlist(eventOpt.get(), userOpt.get());

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Removed from waitlist"));
    }

    @GetMapping("/{eventId}/position")
    public ResponseEntity<?> getPosition(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Security fix:
        // Prevent users from querying another user's waitlist position
        // by supplying arbitrary user IDs.
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        Long userId = userDetails.getId();

        Optional<Event> eventOpt = eventRepository.findById(eventId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (eventOpt.isEmpty())
            return ResponseEntity.notFound().build();
        if (userOpt.isEmpty())
            return ResponseEntity.notFound().build();

        int position = waitlistService.getWaitlistPosition(
                eventOpt.get(),
                userOpt.get());

        return ResponseEntity.ok(
                Map.of(
                        "position", position));
    }

    @GetMapping("/{eventId}/count")
    public ResponseEntity<?> getCount(@PathVariable Long eventId) {
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty())
            return ResponseEntity.notFound().build();

        long count = waitlistService.getWaitlistCount(eventOpt.get());
        return ResponseEntity.ok(Map.of("count", count));
    }
}