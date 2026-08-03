package com.medreminder.auditservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medreminder.auditservice.entity.MedicationAudit;
import com.medreminder.auditservice.repository.MedicationAuditRepository;
import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.dto.DoseMissedEvent;
import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Consumer for Audit Service.
 * Listens for all events and creates audit trails.
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final MedicationAuditRepository medicationAuditRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditEventConsumer(MedicationAuditRepository medicationAuditRepository,
                              ObjectMapper objectMapper) {
        this.medicationAuditRepository = medicationAuditRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles medication-due events from Kafka.
     * Creates audit records for medication due events.
     *
     * @param event MedicationDueEvent from Kafka
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_MEDICATION_DUE,
        groupId = "audit-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleMedicationDueEvent(@Payload MedicationDueEvent event) {
        log.info("Received medication-due event for audit: patientId={}, medicationId={}",
                event.getPatientId(), event.getMedicationId());

        try {
            MedicationAudit audit = createAuditRecord(
                    UUID.fromString(event.getPatientId()),
                    UUID.fromString(event.getMedicationId()),
                    "MEDICATION_DUE",
                    objectMapper.writeValueAsString(event)
            );
            medicationAuditRepository.save(audit);

            log.info("Audit record created for medication-due event: auditId={}, patientId={}",
                    audit.getAuditId(), event.getPatientId());

        } catch (Exception e) {
            log.error("Error creating audit for medication-due event: patientId={}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Handles call-response-received events from Kafka.
     * Creates audit records for call responses.
     *
     * @param event CallResponseEvent from Kafka
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_CALL_RESPONSE,
        groupId = "audit-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleCallResponseEvent(@Payload CallResponseEvent event) {
        log.info("Received call-response event for audit: patientId={}, callId={}",
                event.getPatientId(), event.getCallId());

        try {
            String actionType = event.isResponseReceived() ? "CALL_RESPONSE_TAKEN" : "CALL_RESPONSE_MISSED";
            
            MedicationAudit audit = createAuditRecord(
                    UUID.fromString(event.getPatientId()),
                    UUID.fromString(event.getScheduleId()),
                    actionType,
                    objectMapper.writeValueAsString(event)
            );
            medicationAuditRepository.save(audit);

            log.info("Audit record created for call-response event: auditId={}, patientId={}",
                    audit.getAuditId(), event.getPatientId());

        } catch (Exception e) {
            log.error("Error creating audit for call-response event: patientId={}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Handles dose-missed events from Kafka.
     * Creates audit records for dose missed events.
     *
     * @param event DoseMissedEvent from Kafka
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_DOSE_MISSED,
        groupId = "audit-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDoseMissedEvent(@Payload DoseMissedEvent event) {
        log.info("Received dose-missed event for audit: patientId={}, scheduleId={}",
                event.getPatientId(), event.getScheduleId());

        try {
            MedicationAudit audit = createAuditRecord(
                    UUID.fromString(event.getPatientId()),
                    UUID.fromString(event.getScheduleId()),
                    "DOSE_MISSED",
                    objectMapper.writeValueAsString(event)
            );
            medicationAuditRepository.save(audit);

            log.info("Audit record created for dose-missed event: auditId={}, patientId={}",
                    audit.getAuditId(), event.getPatientId());

        } catch (Exception e) {
            log.error("Error creating audit for dose-missed event: patientId={}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Generic listener for other auditable events.
     * Handles any event payload and creates appropriate audit records.
     *
     * @param payload Raw event payload as string
     * @param topic Topic from which the event originated
     * @param partition Kafka partition header
     * @param offset Kafka offset header
     */
    @KafkaListener(
        topics = {Constants.KAFKA_TOPIC_MEDICATION_DUE, Constants.KAFKA_TOPIC_CALL_RESPONSE, Constants.KAFKA_TOPIC_DOSE_MISSED},
        groupId = "audit-service-generic-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleAllAuditableEvents(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {

        log.info("Received generic event for audit: topic={}, partition={}, offset={}",
                topic, partition, offset);

        try {
            String actionType = determineActionType(payload, topic);
            if (actionType != null) {
                // Try to extract patientId and medicationId from payload
                MedicationAudit audit = new MedicationAudit();
                audit.setActionType(actionType);
                audit.setActionTime(LocalDateTime.now());
                audit.setActionDetails(payload);
                audit.setCreatedAt(LocalDateTime.now());
                
                // Try to extract IDs from payload
                try {
                    // Use Jackson to extract fields from the payload
                    if (topic.equals(Constants.KAFKA_TOPIC_MEDICATION_DUE)) {
                        MedicationDueEvent event = objectMapper.readValue(payload, MedicationDueEvent.class);
                        audit.setPatientId(UUID.fromString(event.getPatientId()));
                        audit.setMedicationId(UUID.fromString(event.getMedicationId()));
                    } else if (topic.equals(Constants.KAFKA_TOPIC_CALL_RESPONSE)) {
                        CallResponseEvent event = objectMapper.readValue(payload, CallResponseEvent.class);
                        audit.setPatientId(UUID.fromString(event.getPatientId()));
                        audit.setMedicationId(UUID.fromString(event.getScheduleId()));
                    } else if (topic.equals(Constants.KAFKA_TOPIC_DOSE_MISSED)) {
                        DoseMissedEvent event = objectMapper.readValue(payload, DoseMissedEvent.class);
                        audit.setPatientId(UUID.fromString(event.getPatientId()));
                        audit.setMedicationId(UUID.fromString(event.getScheduleId()));
                    }
                } catch (Exception e) {
                    log.warn("Could not extract IDs from payload for topic: {}, error: {}", topic, e.getMessage());
                    // Set placeholder UUIDs to avoid null
                    audit.setPatientId(UUID.randomUUID());
                    audit.setMedicationId(UUID.randomUUID());
                }
                
                medicationAuditRepository.save(audit);
                log.info("Generic audit record created for topic: {}, actionType: {}", 
                        topic, actionType);
            }

        } catch (Exception e) {
            log.error("Error processing generic audit event for topic: {}, error: {}",
                    topic, e.getMessage(), e);
        }
    }

    /**
     * Determines the action type based on event payload and topic.
     *
     * @param payload Raw event payload
     * @param topic Kafka topic
     * @return Action type string
     */
    private String determineActionType(String payload, String topic) {
        try {
            if (topic.equals(Constants.KAFKA_TOPIC_MEDICATION_DUE)) {
                MedicationDueEvent event = objectMapper.readValue(payload, MedicationDueEvent.class);
                return "MEDICATION_DUE";
            } else if (topic.equals(Constants.KAFKA_TOPIC_CALL_RESPONSE)) {
                CallResponseEvent event = objectMapper.readValue(payload, CallResponseEvent.class);
                return event.isResponseReceived() ? "CALL_RESPONSE_TAKEN" : "CALL_RESPONSE_MISSED";
            } else if (topic.equals(Constants.KAFKA_TOPIC_DOSE_MISSED)) {
                DoseMissedEvent event = objectMapper.readValue(payload, DoseMissedEvent.class);
                return "DOSE_MISSED";
            }
        } catch (Exception e) {
            log.warn("Failed to parse payload for topic: {}, error: {}", topic, e.getMessage());
        }
        return null;
    }

    /**
     * Creates an audit record for an event.
     *
     * @param patientId UUID of patient
     * @param entityId UUID of related entity (medication or schedule)
     * @param actionType Type of action performed
     * @param details JSON string with event details
     * @return MedicationAudit entity
     */
    private MedicationAudit createAuditRecord(UUID patientId, UUID entityId, 
                                              String actionType, String details) {
        MedicationAudit audit = new MedicationAudit();
        audit.setPatientId(patientId);
        audit.setMedicationId(entityId); // Could be medicationId or scheduleId
        audit.setActionType(actionType);
        audit.setActionTime(LocalDateTime.now());
        audit.setActionDetails(details);
        audit.setCreatedAt(LocalDateTime.now());
        return audit;
    }
}
