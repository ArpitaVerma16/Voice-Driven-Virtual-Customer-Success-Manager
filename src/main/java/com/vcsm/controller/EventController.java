package com.vcsm.controller;

import com.vcsm.model.Event;
import com.vcsm.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.vcsm.security.service.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    @Autowired
    private EventService eventService;

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Event> create(@Valid @RequestBody Event event) {
        return ResponseEntity.ok(eventService.createEvent(event));
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAll() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Event>> getActive() {
        return ResponseEntity.ok(eventService.getActiveEvents());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Event>> getUpcoming() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Event>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(eventService.getEventsByCategory(
                Event.EventCategory.valueOf(category.toUpperCase())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getById(@PathVariable Long id) {
        return eventService.getEventById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Event> update(@PathVariable Long id, @Valid @RequestBody Event event) {
        return ResponseEntity.ok(eventService.updateEvent(id, event));
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<?> register(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Security fix:
        // Always use the authenticated user's profile ID instead of
        // accepting a userId from the client request. This prevents
        // users from registering events on behalf of other accounts.
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unable to resolve authenticated user");
        }

        Long resolvedUserId = userDetails.getId();

        try {
            return ResponseEntity.ok(
                    eventService.registerForEvent(id, resolvedUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<Event>> recommend(@RequestParam String keyword) {
        return ResponseEntity.ok(eventService.recommendEvents(keyword));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

}