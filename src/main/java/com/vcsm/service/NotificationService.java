package com.vcsm.service;

import com.vcsm.model.Notification;
import com.vcsm.model.User;
import com.vcsm.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification(user, title, message, type);
        return notificationRepository.save(notification);
    }
    
    public void sendNotification(User user, Notification notification) {
        // Save to database
        notificationRepository.save(notification);
        
        // Send via WebSocket
        messagingTemplate.convertAndSendToUser(
            user.getId().toString(),
            "/queue/notifications",
            notification
        );
    }
    
    public void sendGlobalNotification(Notification notification) {
        notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/global", notification);
    }
    
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
    }
    
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }
    
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
    
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}