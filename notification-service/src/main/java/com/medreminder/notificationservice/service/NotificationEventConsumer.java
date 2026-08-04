package com.medreminder.notificationservice.service;

import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.notificationservice.entity.Notification;
import com.medreminder.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationRepository notificationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public NotificationEventConsumer(NotificationRepository notificationRepository,
                                     RabbitTemplate rabbitTemplate) {
        this.notificationRepository = notificationRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_MEDICATION_DUE,
        groupId = "notification-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleMedicationDueEvent(
            @Payload MedicationDueEvent event,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {

        log.info("Received medication-due event: patientId={}, medicationId={}, scheduledTime={}, partition={}, offset={}",
                event.getPatientId(), event.getMedicationId(), event.getScheduledTime(), partition, offset);

        try {
            UUID patientId = event.getPatientId();
            String medicationIdStr = event.getMedicationId() != null ? event.getMedicationId().toString() : "";

            boolean emailPreferred = true;
            boolean smsPreferred = true;

            String emailMessage = buildEmailMessage(event);
            String smsMessage = buildSmsMessage(event);

            if (emailPreferred) {
                sendEmailNotification(patientId, medicationIdStr, emailMessage);
            }

            if (smsPreferred) {
                sendSmsNotification(patientId, medicationIdStr, smsMessage);
            }

            log.info("Successfully processed medication-due event for patientId: {}, notifications sent: email={}, sms={}",
                    patientId, emailPreferred, smsPreferred);

        } catch (Exception e) {
            log.error("Error processing medication-due event for patientId: {}, medicationId: {}, error: {}",
                    event.getPatientId(), event.getMedicationId(), e.getMessage(), e);
        }
    }

    private void sendEmailNotification(UUID patientId, String medicationId, String message) {
        try {
            Notification notification = createNotification(patientId, "EMAIL", message, "email");
            notification.setSubject("Medication Reminder - Take Your Medication");
            notificationRepository.save(notification);

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("notificationId", notification.getNotificationId().toString());
            emailData.put("recipient", "patient@example.com");
            emailData.put("subject", notification.getSubject());
            emailData.put("message", message);

            rabbitTemplate.convertAndSend(Constants.RABBITMQ_QUEUE_EMAIL, emailData);

            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Email notification published to RabbitMQ: notificationId={}, patientId={}",
                    notification.getNotificationId(), patientId);

        } catch (AmqpException e) {
            log.error("Failed to publish email notification to RabbitMQ: patientId={}, error: {}",
                    patientId, e.getMessage(), e);
            updateNotificationStatus(patientId, "FAILED", e.getMessage());
        }
    }

    private void sendSmsNotification(UUID patientId, String medicationId, String message) {
        try {
            Notification notification = createNotification(patientId, "SMS", message, "sms");
            notification.setSubject("SMS Reminder");
            notificationRepository.save(notification);

            Map<String, Object> smsData = new HashMap<>();
            smsData.put("notificationId", notification.getNotificationId().toString());
            smsData.put("recipient", "+1234567890");
            smsData.put("message", message);

            rabbitTemplate.convertAndSend(Constants.RABBITMQ_QUEUE_SMS, smsData);

            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("SMS notification published to RabbitMQ: notificationId={}, patientId={}",
                    notification.getNotificationId(), patientId);

        } catch (AmqpException e) {
            log.error("Failed to publish SMS notification to RabbitMQ: patientId={}, error: {}",
                    patientId, e.getMessage(), e);
            updateNotificationStatus(patientId, "FAILED", e.getMessage());
        }
    }

    private Notification createNotification(UUID patientId, String type, String message, String recipient) {
        Notification notification = new Notification();
        notification.setPatientId(patientId);
        notification.setNotificationType(type);
        notification.setRecipient(recipient);
        notification.setBody(message);
        notification.setStatus("PENDING");
        notification.setRetryCount(0);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        return notification;
    }

    private void updateNotificationStatus(UUID patientId, String status, String errorMessage) {
        try {
            Notification notification = notificationRepository
                    .findFirstByPatientIdOrderByCreatedAtDesc(patientId)
                    .orElse(null);

            if (notification != null) {
                notification.setStatus(status);
                notification.setFailureReason(errorMessage);
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            log.error("Failed to update notification status for patientId: {}, error: {}",
                    patientId, e.getMessage(), e);
        }
    }

    private String buildEmailMessage(MedicationDueEvent event) {
        return String.format(
                "Dear Patient,\n\n" +
                "This is a reminder from %s Hospital to take your medication.\n\n" +
                "Medication ID: %s\n" +
                "Scheduled Time: %s\n" +
                "Location: %s\n\n" +
                "Please take your medication as prescribed.\n\n" +
                "Thank you,\n" +
                "%s Hospital Team",
                "City Hospital",
                event.getMedicationId(),
                event.getScheduledTime(),
                event.getLocation(),
                "City Hospital"
        );
    }

    private String buildSmsMessage(MedicationDueEvent event) {
        String message = String.format(
                "Medication reminder: Take your medication at %s. - %s Hospital",
                event.getScheduledTime(),
                "City Hospital"
        );

        if (message.length() > 160) {
            message = message.substring(0, 157) + "...";
        }
        return message;
    }
}
