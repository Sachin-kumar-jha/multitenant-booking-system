package com.ticket.notification.service;

import com.ticket.notification.entity.*;
import com.ticket.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${notification.email.simulation.enabled:true}")
    private boolean simulationEnabled;

    @Value("${notification.email.from:noreply@ticketbooking.com}")
    private String fromEmail;

    @Value("${notification.email.from-name:Ticket Booking Platform}")
    private String fromName;

    public NotificationService(NotificationRepository notificationRepository,
                              JavaMailSender mailSender,
                              TemplateEngine templateEngine) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Transactional
    public Notification sendBookingConfirmation(Long userId, String userEmail, String userName,
                                                Long bookingId, String confirmationNumber,
                                                String eventName, LocalDateTime eventDate,
                                                String venue, List<Long> seatIds,
                                                BigDecimal amount, String paymentReference) {
        logger.info("Sending booking confirmation to {}", userEmail);

        // Check for duplicate
        if (notificationRepository.existsByReferenceTypeAndReferenceIdAndNotificationType(
                "booking", bookingId, NotificationType.BOOKING_CONFIRMATION)) {
            logger.warn("Booking confirmation already sent for booking {}", bookingId);
            return null;
        }

        String subject = "Booking Confirmed - " + confirmationNumber;
        String content = buildBookingConfirmationContent(userName, confirmationNumber, 
                eventName, eventDate, venue, seatIds.size(), amount, paymentReference);

        Notification notification = createNotification(
            userId, userEmail, NotificationType.BOOKING_CONFIRMATION,
            subject, content, "booking", bookingId
        );

        sendNotification(notification);
        return notification;
    }

    @Transactional
    public Notification sendBookingCancellation(Long userId, String userEmail, 
                                                Long bookingId, String confirmationNumber,
                                                String reason) {
        logger.info("Sending booking cancellation to {}", userEmail);

        String subject = "Booking Cancelled - " + confirmationNumber;
        String content = buildBookingCancellationContent(confirmationNumber, reason);

        Notification notification = createNotification(
            userId, userEmail, NotificationType.BOOKING_CANCELLED,
            subject, content, "booking", bookingId
        );

        sendNotification(notification);
        return notification;
    }

    @Transactional
    public Notification sendPaymentConfirmation(Long userId, String userEmail,
                                                Long paymentId, BigDecimal amount,
                                                String transactionReference) {
        logger.info("Sending payment confirmation to {}", userEmail);

        String subject = "Payment Received - " + transactionReference;
        String content = buildPaymentConfirmationContent(amount, transactionReference);

        Notification notification = createNotification(
            userId, userEmail, NotificationType.PAYMENT_RECEIVED,
            subject, content, "payment", paymentId
        );

        sendNotification(notification);
        return notification;
    }

    @Transactional
    public Notification sendPaymentFailed(Long userId, String userEmail,
                                          Long bookingId, String reason) {
        logger.info("Sending payment failed notification to {}", userEmail);

        String subject = "Payment Failed - Action Required";
        String content = buildPaymentFailedContent(reason);

        Notification notification = createNotification(
            userId, userEmail, NotificationType.PAYMENT_FAILED,
            subject, content, "booking", bookingId
        );

        sendNotification(notification);
        return notification;
    }

    @Transactional
    public Notification sendEventCancellation(Long userId, String userEmail,
                                              Long eventId, String eventName,
                                              String reason) {
        logger.info("Sending event cancellation to {}", userEmail);

        String subject = "Event Cancelled - " + eventName;
        String content = buildEventCancellationContent(eventName, reason);

        Notification notification = createNotification(
            userId, userEmail, NotificationType.EVENT_CANCELLED,
            subject, content, "event", eventId
        );

        sendNotification(notification);
        return notification;
    }

    private Notification createNotification(Long userId, String userEmail,
                                           NotificationType type, String subject,
                                           String content, String refType, Long refId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserEmail(userEmail);
        notification.setNotificationType(type);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setSubject(subject);
        notification.setContent(content);
        notification.setReferenceType(refType);
        notification.setReferenceId(refId);
        notification.setStatus(NotificationStatus.PENDING);
        return notificationRepository.save(notification);
    }

    @Async
    public void sendNotification(Notification notification) {
        try {
            notification.setStatus(NotificationStatus.SENDING);
            notificationRepository.save(notification);

            if (simulationEnabled) {
                simulateSending(notification);
            } else {
                sendEmail(notification);
            }

            notification.markSent();
            notificationRepository.save(notification);
            logger.info("Notification {} sent successfully to {}", 
                       notification.getId(), notification.getUserEmail());
        } catch (Exception e) {
            logger.error("Failed to send notification {}: {}", 
                        notification.getId(), e.getMessage());
            notification.markFailed(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    private void simulateSending(Notification notification) {
        logger.info("SIMULATED EMAIL SEND:");
        logger.info("  To: {}", notification.getUserEmail());
        logger.info("  Subject: {}", notification.getSubject());
        logger.info("  Content: {}", notification.getContent().substring(0, 
                Math.min(200, notification.getContent().length())) + "...");
        
        try {
            Thread.sleep(100); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendEmail(Notification notification) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, fromName);
        helper.setTo(notification.getUserEmail());
        helper.setSubject(notification.getSubject());
        helper.setText(notification.getContent(), true);

        mailSender.send(message);
    }

    // Content builders
    private String buildBookingConfirmationContent(String userName, String confirmationNumber,
                                                    String eventName, LocalDateTime eventDate,
                                                    String venue, int ticketCount,
                                                    BigDecimal amount, String paymentReference) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");
        
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h1 style="color: #2563eb;">Booking Confirmed!</h1>
                <p>Dear %s,</p>
                <p>Your booking has been confirmed. Here are your ticket details:</p>
                
                <div style="background-color: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0;">
                    <h2 style="margin-top: 0;">%s</h2>
                    <p><strong>Confirmation Number:</strong> %s</p>
                    <p><strong>Date:</strong> %s</p>
                    <p><strong>Venue:</strong> %s</p>
                    <p><strong>Tickets:</strong> %d</p>
                    <p><strong>Total Paid:</strong> $%.2f</p>
                    <p><strong>Payment Reference:</strong> %s</p>
                </div>
                
                <p>Please present this confirmation number at the venue.</p>
                <p>Thank you for your purchase!</p>
                
                <hr style="border: 1px solid #e5e7eb; margin: 30px 0;">
                <p style="color: #6b7280; font-size: 12px;">
                    This email was sent by Ticket Booking Platform.
                </p>
            </body>
            </html>
            """,
            userName != null ? userName : "Customer",
            eventName,
            confirmationNumber,
            eventDate.format(formatter),
            venue,
            ticketCount,
            amount,
            paymentReference
        );
    }

    private String buildBookingCancellationContent(String confirmationNumber, String reason) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h1 style="color: #dc2626;">Booking Cancelled</h1>
                <p>Your booking with confirmation number <strong>%s</strong> has been cancelled.</p>
                <p><strong>Reason:</strong> %s</p>
                <p>If you did not request this cancellation or have any questions, please contact our support team.</p>
                <p>If applicable, your refund will be processed within 5-10 business days.</p>
            </body>
            </html>
            """, confirmationNumber, reason);
    }

    private String buildPaymentConfirmationContent(BigDecimal amount, String transactionReference) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h1 style="color: #059669;">Payment Received</h1>
                <p>We have received your payment.</p>
                <div style="background-color: #f3f4f6; padding: 20px; border-radius: 8px;">
                    <p><strong>Amount:</strong> $%.2f</p>
                    <p><strong>Transaction Reference:</strong> %s</p>
                </div>
                <p>Thank you for your payment!</p>
            </body>
            </html>
            """, amount, transactionReference);
    }

    private String buildPaymentFailedContent(String reason) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h1 style="color: #dc2626;">Payment Failed</h1>
                <p>Unfortunately, your payment could not be processed.</p>
                <p><strong>Reason:</strong> %s</p>
                <p>Please try again with a different payment method or contact your bank.</p>
                <p>Your seats have been released and you will need to select them again.</p>
            </body>
            </html>
            """, reason);
    }

    private String buildEventCancellationContent(String eventName, String reason) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h1 style="color: #dc2626;">Event Cancelled</h1>
                <p>We regret to inform you that <strong>%s</strong> has been cancelled.</p>
                <p><strong>Reason:</strong> %s</p>
                <p>A full refund will be processed within 5-10 business days.</p>
                <p>We apologize for any inconvenience caused.</p>
            </body>
            </html>
            """, eventName, reason);
    }

    // Scheduled retry for failed notifications
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void retryFailedNotifications() {
        List<Notification> notifications = notificationRepository
                .findPendingOrFailedForRetry(MAX_RETRIES);

        for (Notification notification : notifications) {
            logger.info("Retrying notification {}", notification.getId());
            notification.markRetrying();
            sendNotification(notification);
        }
    }
}
