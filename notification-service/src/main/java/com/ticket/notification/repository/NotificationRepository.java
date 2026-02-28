package com.ticket.notification.repository;

import com.ticket.notification.entity.Notification;
import com.ticket.notification.entity.NotificationChannel;
import com.ticket.notification.entity.NotificationStatus;
import com.ticket.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status, Pageable pageable);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndNotificationType(Long userId, NotificationType notificationType);

    List<Notification> findByUserIdAndChannel(Long userId, NotificationChannel channel);

    List<Notification> findByUserIdAndNotificationTypeAndChannel(Long userId, NotificationType notificationType, NotificationChannel channel);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.read = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' OR (n.status = 'FAILED' AND n.retryCount < :maxRetries)")
    List<Notification> findPendingOrFailedForRetry(@Param("maxRetries") int maxRetries);

    @Query("SELECT n FROM Notification n WHERE n.referenceType = :refType AND n.referenceId = :refId")
    List<Notification> findByReference(@Param("refType") String referenceType, @Param("refId") Long referenceId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.createdAt > :since")
    long countByStatusSince(@Param("status") NotificationStatus status, @Param("since") LocalDateTime since);

    boolean existsByReferenceTypeAndReferenceIdAndNotificationType(
            String referenceType, Long referenceId, NotificationType notificationType);
}
