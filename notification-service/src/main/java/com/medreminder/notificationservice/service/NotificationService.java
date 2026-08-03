package com.medreminder.notificationservice.service;
import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.notificationservice.dto.NotificationResponse;
import com.medreminder.notificationservice.dto.SendNotificationRequest;
import com.medreminder.notificationservice.entity.Notification;
import com.medreminder.notificationservice.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@Slf4j
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private SmsService smsService;
    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        log.info("Sending {} notification to: {}", request.getNotificationType(), request.getRecipient());
        Notification notification = new Notification();
        notification.setPatientId(request.getPatientId());
        notification.setNotificationType(request.getNotificationType());
        notification.setRecipient(request.getRecipient());
        notification.setSubject(request.getSubject());
        notification.setBody(request.getBody());
        notification.setStatus("PENDING");
        boolean success = false;
        if ("EMAIL".equalsIgnoreCase(request.getNotificationType())) {
            success = emailService.sendEmail(request.getRecipient(), request.getSubject(), request.getBody());
        } else if ("SMS".equalsIgnoreCase(request.getNotificationType())) {
            success = smsService.sendSms(request.getRecipient(), request.getBody());
        }
        if (success) {
            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
            log.info("Notification sent successfully");
        } else {
            notification.setStatus("FAILED");
            notification.setFailureReason("Delivery provider error");
            log.warn("Notification send failed");
        }
        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }
    @KafkaListener(topics = "medication-due", groupId = "notification-service-group")
    @Transactional
    public void handleMedicationDueEvent(MedicationDueEvent event) {
        log.info("Received medication-due event for patient: {}", event.getPatientId());
        try {
            String subject = "Medication Reminder: " + event.getMedicationName();
            String body = String.format("It's time to take your medication: %s (%s) at %s", event.getMedicationName(), event.getDosage(), event.getScheduledTime());
            SendNotificationRequest smsRequest = new SendNotificationRequest();
            smsRequest.setPatientId(event.getPatientId());
            smsRequest.setNotificationType("SMS");
            smsRequest.setRecipient(event.getPatientPhoneNumber());
            smsRequest.setSubject(subject);
            smsRequest.setBody(body);
            sendNotification(smsRequest);
            log.info("Medication reminder sent for patient: {}", event.getPatientId());
        } catch (Exception e) {
            log.error("Error processing medication-due event: {}", e.getMessage(), e);
        }
    }
    public NotificationResponse getNotification(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        return mapToResponse(notification);
    }
    public List<NotificationResponse> getPatientNotifications(UUID patientId) {
        List<Notification> notifications = notificationRepository.findByPatientId(patientId);
        return notifications.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    public List<NotificationResponse> getPendingNotifications() {
        List<Notification> notifications = notificationRepository.findPendingNotifications();
        return notifications.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(notification.getNotificationId(), notification.getPatientId(), notification.getNotificationType(), notification.getRecipient(), notification.getStatus(), notification.getCreatedAt(), notification.getSentAt());
    }
}
