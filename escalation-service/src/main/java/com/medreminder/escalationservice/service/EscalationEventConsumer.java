package com.medreminder.escalationservice.service;

import com.medreminder.common.dto.DoseMissedEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.escalationservice.entity.Escalation;
import com.medreminder.escalationservice.repository.EscalationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka Consumer for Escalation Service.
 * Listens for dose-missed events and triggers escalation workflows.
 */
@Component
public class EscalationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EscalationEventConsumer.class);

    private final EscalationRepository escalationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public EscalationEventConsumer(EscalationRepository escalationRepository,
                                   RabbitTemplate rabbitTemplate) {
        this.escalationRepository = escalationRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Handles dose-missed events from Kafka.
     * Creates escalation records and triggers escalation workflows.
     *
     * @param event DoseMissedEvent from Kafka
     * @param partition Kafka partition header
     * @param offset Kafka offset header
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_DOSE_MISSED,
        groupId = "escalation-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDoseMissedEvent(
            @Payload DoseMissedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {

        log.info("Received dose-missed event: patientId={}, scheduleId={}, missedTime={}, partition={}, offset={}",
                event.getPatientId(), event.getScheduleId(), event.getMissedTime(), partition, offset);

        try {
            UUID patientId = UUID.fromString(event.getPatientId());
            UUID scheduleId = UUID.fromString(event.getScheduleId());

            // Determine current escalation level for patient
            int currentLevel = getCurrentEscalationLevel(patientId);
            int nextLevel = Math.min(currentLevel + 1, 3);

            // Create escalation entity
            Escalation escalation = new Escalation();
            escalation.setPatientId(patientId);
            escalation.setScheduleId(scheduleId);
            escalation.setEscalationType("DOSE_MISSED");
            escalation.setEscalationLevel(nextLevel);
            escalation.setEscalatedTo(getEscalationTarget(nextLevel));
            escalation.setEscalationTime(LocalDateTime.now());
            escalation.setStatus("PENDING");
            escalation.setCreatedAt(LocalDateTime.now());
            escalation.setUpdatedAt(LocalDateTime.now());

            escalationRepository.save(escalation);

            // Determine escalation action
            String action = determineEscalationAction(nextLevel);
            log.info("Escalation created for patientId: {}, level: {}, action: {}",
                    patientId, nextLevel, action);

            // Publish to RabbitMQ for further processing
            publishEscalationToRabbitMQ(escalation);

            log.info("Successfully processed dose-missed event for patientId: {}, scheduleId: {}, escalationLevel: {}",
                    patientId, scheduleId, nextLevel);

        } catch (Exception e) {
            log.error("Error processing dose-missed event for patientId: {}, scheduleId: {}, error: {}",
                    event.getPatientId(), event.getScheduleId(), e.getMessage(), e);
            // Don't rethrow - let Kafka commit the offset
        }
    }

    /**
     * Determines the current escalation level for a patient.
     *
     * @param patientId UUID of patient
     * @return Current escalation level (0 if none)
     */
    private int getCurrentEscalationLevel(UUID patientId) {
        List<Escalation> recentEscalations = escalationRepository
                .findByPatientIdOrderByCreatedAtDesc(patientId);

        if (recentEscalations.isEmpty()) {
            return 0;
        }

        // Get the highest level from recent escalations (last 24 hours)
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        return recentEscalations.stream()
                .filter(e -> e.getCreatedAt().isAfter(twentyFourHoursAgo))
                .mapToInt(Escalation::getEscalationLevel)
                .max()
                .orElse(0);
    }

    /**
     * Gets the escalation target based on level.
     *
     * @param level Escalation level (1-3)
     * @return Target contact
     */
    private String getEscalationTarget(int level) {
        switch (level) {
            case 1:
                return "CAREGIVER_SMS";
            case 2:
                return "CAREGIVER_CALL";
            case 3:
                return "HOSPITAL_STAFF";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Determines the escalation action based on level.
     *
     * @param level Escalation level (1-3)
     * @return Action description
     */
    public String determineEscalationAction(int level) {
        switch (level) {
            case 1:
                return "Send SMS alert to caregiver";
            case 2:
                return "Make phone call to caregiver";
            case 3:
                return "Notify hospital staff";
            default:
                return "Unknown escalation level";
        }
    }

    /**
     * Publishes escalation message to RabbitMQ escalation queue.
     *
     * @param escalation Escalation entity
     */
    private void publishEscalationToRabbitMQ(Escalation escalation) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("escalationId", escalation.getEscalationId().toString());
            message.put("patientId", escalation.getPatientId().toString());
            message.put("scheduleId", escalation.getScheduleId().toString());
            message.put("escalationLevel", escalation.getEscalationLevel());
            message.put("escalationType", escalation.getEscalationType());
            message.put("status", escalation.getStatus());
            message.put("timestamp", escalation.getEscalationTime().toString());
            message.put("action", determineEscalationAction(escalation.getEscalationLevel()));

            rabbitTemplate.convertAndSend(Constants.RABBITMQ_ESCALATION_QUEUE, message);

            log.info("Successfully published escalation to RabbitMQ queue: {}, escalationId: {}",
                    Constants.RABBITMQ_ESCALATION_QUEUE, escalation.getEscalationId());

        } catch (Exception e) {
            log.error("Failed to publish escalation to RabbitMQ queue: {}, escalationId: {}, error: {}",
                    Constants.RABBITMQ_ESCALATION_QUEUE, escalation.getEscalationId(), e.getMessage(), e);
            // Don't rethrow - escalation is already saved
        }
    }
}
