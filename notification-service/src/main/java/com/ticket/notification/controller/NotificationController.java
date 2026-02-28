package com.ticket.notification.controller;

import com.ticket.notification.entity.Notification;
import com.ticket.notification.entity.NotificationChannel;
import com.ticket.notification.entity.NotificationType;
import com.ticket.notification.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationChannel channel) {
        
        List<Notification> notifications;
        if (type != null && channel != null) {
            notifications = notificationRepository.findByUserIdAndNotificationTypeAndChannel(userId, type, channel);
        } else if (type != null) {
            notifications = notificationRepository.findByUserIdAndNotificationType(userId, type);
        } else if (channel != null) {
            notifications = notificationRepository.findByUserIdAndChannel(userId, channel);
        } else {
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<Notification> getNotification(@PathVariable Long notificationId) {
        return notificationRepository.findById(notificationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationRepository.findUnreadByUserId(userId));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    notification.setRead(true);
                    return ResponseEntity.ok(notificationRepository.save(notification));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        List<Notification> unread = notificationRepository.findUnreadByUserId(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/count")
    public ResponseEntity<NotificationCount> getNotificationCount(@PathVariable Long userId) {
        long total = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).size();
        long unread = notificationRepository.findUnreadByUserId(userId).size();
        return ResponseEntity.ok(new NotificationCount(total, unread));
    }

    public record NotificationCount(long total, long unread) {}
}
