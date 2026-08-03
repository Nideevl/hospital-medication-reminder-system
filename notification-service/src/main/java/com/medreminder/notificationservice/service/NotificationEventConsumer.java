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

/**
 * Kafka Consumer for Notification Service.
 * Listens for medication-due events and sends notifications via RabbitMQ.
 */
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

    /**
     * Handles medication-due events from Kafka.
     * Sends notifications based on patient preferences.
     *
     * @param event MedicationDueEvent from Kafka
     * @param partition Kafka partition header
     * @param offset Kafka offset header
     */
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
            UUID patientId = UUID.fromString(event.getPatientId());
            
            // In a real implementation, fetch patient preferences from database
            // For now, we'll send both email and SMS
            boolean emailPreferred = true;
            boolean smsPreferred = true;

            // Build notification messages
            String emailMessage = buildEmailMessage(event);
            String smsMessage = buildSmsMessage(event);

            // Send email notification
            if (emailPreferred) {
                sendEmailNotification(patientId, event.getMedicationId(), emailMessage);
            }

            // Send SMS notification
            if (smsPreferred) {
                sendSmsNotification(patientId, event.getMedicationId(), smsMessage);
            }

            log.info("Successfully processed medication-due event for patientId: {}, notifications sent: email={}, sms={}",
                    patientId, emailPreferred, smsPreferred);

        } catch (Exception e) {
            log.error("Error processing medication-due event for patientId: {}, medicationId: {}, error: {}",
                    event.getPatientId(), event.getMedicationId(), e.getMessage(), e);
            // Don't rethrow - let Kafka commit the offset
        }
    }

    /**
     * Sends email notification by publishing to RabbitMQ email queue.
     *
     * @param patientId UUID of patient
     * @param medicationId UUID of medication
     * @param message Email content
     */
    private void sendEmailNotification(UUID patientId, String medicationId, String message) {
        try {
            Notification notification = createNotification(patientId, "EMAIL", message, "email");
            notification.setSubject("Medication Reminder - Take Your Medication");
            notificationRepository.save(notification);

            // Publish to RabbitMQ email queue
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("notificationId", notification.getNotificationId().toString());
            emailData.put("recipient", "patient@example.com"); // Would be fetched from patient service
            emailData.put("subject", notification.getSubject());
            emailData.put("message", message);

            rabbitTemplate.convertAndSend(Constants.RABBITMQ_EMAIL_QUEUE, emailData);

            notification.setStatus("SENT");
            notification.setSentTime(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Email notification published to RabbitMQ: notificationId={}, patientId={}",
                    notification.getNotificationId(), patientId);

        } catch (AmqpException e) {
            log.error("Failed to publish email notification to RabbitMQ: patientId={}, error: {}",
                    patientId, e.getMessage(), e);
            updateNotificationStatus(patientId, "FAILED", e.getMessage());
        }
    }

    /**
     * Sends SMS notification by publishing to RabbitMQ SMS queue.
     *
     * @param patientId UUID of patient
     * @param medicationId UUID of medication
     * @param message SMS content
     */
    private void sendSmsNotification(UUID patientId, String medicationId, String message) {
        try {
            Notification notification = createNotification(patientId, "SMS", message, "sms");
            notificationRepository.save(notification);

            // Publish to RabbitMQ SMS queue
            Map<String, Object> smsData = new HashMap<>();
            smsData.put("notificationId", notification.getNotificationId().toString());
            smsData.put("recipient", "+1234567890"); // Would be fetched from patient service
            smsData.put("message", message);

            rabbitTemplate.convertAndSend(Constants.RABBITMQ_SMS_QUEUE, smsData);

            notification.setStatus("SENT");
            notification.setSentTime(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("SMS notification published to RabbitMQ: notificationId={}, patientId={}",
                    notification.getNotificationId(), patientId);

        } catch (AmqpException e) {
            log.error("Failed to publish SMS notification to RabbitMQ: patientId={}, error: {}",
                    patientId, e.getMessage(), e);
            updateNotificationStatus(patientId, "FAILED", e.getMessage());
        }
    }

    /**
     * Creates a Notification entity.
     *
     * @param patientId UUID of patient
     * @param type Notification type (EMAIL/SMS)
     * @param message Notification content
     * @param recipient Recipient identifier
     * @return Notification entity
     */
    private Notification createNotification(UUID patientId, String type, String message, String recipient) {
        Notification notification = new Notification();
        notification.setPatientId(patientId);
        notification.setNotificationType(type);
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setStatus("PENDING");
        notification.setRetryCount(0);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        return notification;
    }

    /**
     * Updates notification status when sending fails.
     *
     * @param patientId UUID of patient
     * @param status Status to set
     * @param errorMessage Error message
     */
    private void updateNotificationStatus(UUID patientId, String status, String errorMessage) {
        try {
            Notification notification = notificationRepository
                    .findFirstByPatientIdOrderByCreatedAtDesc(patientId)
                    .orElse(null);
            
            if (notification != null) {
                notification.setStatus(status);
                notification.setErrorMessage(errorMessage);
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            log.error("Failed to update notification status for patientId: {}, error: {}",
                    patientId, e.getMessage(), e);
        }
    }

    /**
     * Builds a professional email message.
     *
     * @param event MedicationDueEvent
     * @return Email body
     */
    private String buildEmailMessage(MedicationDueEvent event) {
        return String.format(
                "Dear Patient,\n\n" +
                "This is a reminder from %s Hospital to take your medication.\n\n" +
                "Medication: %s\n" +
                "Scheduled Time: %s\n" +
                "Location: %s\n\n" +
                "Please take your medication as prescribed. If you have any questions,\n" +
                "please contact your healthcare provider.\n\n" +
                "Thank you,\n" +
                "%s Hospital Team",
                "City Hospital", // Hospital name from context
                event.getMedicationId(),
                event.getScheduledTime(),
                event.getLocation(),
                "City Hospital"
        );
    }

    /**
     * Builds a concise SMS message.
     *
     * @param event MedicationDueEvent
     * @return SMS text (under 160 characters)
     */
    private String buildSmsMessage(MedicationDueEvent event) {
        String message = String.format(
                "Medication reminder: Take your medication at %s. - %s Hospital",
                event.getScheduledTime(),
                "City Hospital"
        );
        
        // Truncate if over 160 characters
        if (message.length() > 160) {
            message = message.substring(0, 157) + "...";
        }
        return message;
    }
}
