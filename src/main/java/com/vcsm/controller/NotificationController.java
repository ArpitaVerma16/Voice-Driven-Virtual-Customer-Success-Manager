package com.vcsm.controller;

import com.vcsm.model.Notification;
import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@lombok.RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    private final UserRepository userRepository;
    
    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
    
    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationService.getUserNotifications(user));
    }
    
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationService.getUnreadNotifications(user));
    }
    
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Map<String, Long> response = new HashMap<>();
        response.put("count", notificationService.getUnreadCount(user));
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }
    
    @MessageMapping("/notification")
    @SendTo("/topic/global")
    public Notification sendGlobalNotification(Notification notification) {
        return notification;
    }
}