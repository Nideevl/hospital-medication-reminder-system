#!/bin/bash

# Create all Kafka consumer listener and event publisher files

# Function to create directory if it doesn't exist
create_dir() {
    if [ ! -d "$1" ]; then
        mkdir -p "$1"
        echo "Created directory: $1"
    fi
}

# Function to create file with content
create_file() {
    local file_path="$1"
    local content="$2"
    
    # Create directory if it doesn't exist
    local dir_path=$(dirname "$file_path")
    create_dir "$dir_path"
    
    # Write content to file
    echo "$content" > "$file_path"
    echo "Created file: $file_path"
}

echo "Starting creation of Kafka consumer listener files..."

# ============================================
# FILE 1: Schedule Service - Kafka Consumer Listener
# ============================================

create_file "schedule-service/src/main/java/com/medreminder/scheduleservice/service/MedicationEventConsumer.java" << 'EOF'
package com.medreminder.scheduleservice.service;

import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.scheduleservice.entity.Schedule;
import com.medreminder.scheduleservice.exception.ResourceNotFoundException;
import com.medreminder.scheduleservice.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Consumer for Schedule Service.
 * Listens for call-response-received events and updates schedule status.
 */
@Component
public class MedicationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MedicationEventConsumer.class);

    private final ScheduleRepository scheduleRepository;

    @Autowired
    public MedicationEventConsumer(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * Handles call-response-received events from Kafka.
     * Updates schedule status based on the call response.
     *
     * @param event CallResponseEvent from Kafka
     * @param partition Kafka partition header (for debugging)
     * @param offset Kafka offset header (for debugging)
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_CALL_RESPONSE,
        groupId = "schedule-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCallResponse(
            @Payload CallResponseEvent event,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {

        log.info("Received call-response-received event: scheduleId={}, patientId={}, responseReceived={}, partition={}, offset={}",
                event.getScheduleId(), event.getPatientId(), event.isResponseReceived(), partition, offset);

        try {
            UUID scheduleId = UUID.fromString(event.getScheduleId());
            
            // Find schedule by ID
            Schedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

            // Update schedule based on response
            boolean isTaken = event.isResponseReceived();
            schedule.setActive(!isTaken); // If taken, deactivate the schedule
            schedule.setUpdatedAt(LocalDateTime.now());

            scheduleRepository.save(schedule);

            log.info("Successfully processed call response for scheduleId: {}, responseReceived: {}, active: {}",
                    scheduleId, isTaken, schedule.isActive());

        } catch (ResourceNotFoundException e) {
            log.error("Schedule not found for event: scheduleId={}, error: {}", 
                    event.getScheduleId(), e.getMessage());
            // Don't rethrow - let Kafka commit the offset
        } catch (Exception e) {
            log.error("Error processing call response event for scheduleId: {}, error: {}",
                    event.getScheduleId(), e.getMessage(), e);
            // Don't rethrow - let Kafka commit the offset
        }
    }
}
EOF

# ============================================
# FILE 2: Call Service - Kafka Event Publisher
# ============================================

create_file "call-service/src/main/java/com/medreminder/callservice/service/CallEventPublisher.java" 'package com.medreminder.callservice.service;

import com.medreminder.callservice.entity.CallLog;
import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Event Publisher for Call Service.
 * Publishes call-response-received events when call responses are received.
 */
@Component
public class CallEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CallEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public CallEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a call-response-received event based on call log.
     *
     * @param callLog CallLog entity containing call details
     */
    public void publishCallResponse(CallLog callLog) {
        try {
            CallResponseEvent event = buildCallResponseEvent(callLog, null);
            
            Message<CallResponseEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, Constants.KAFKA_TOPIC_CALL_RESPONSE)
                    .setHeader(KafkaHeaders.KEY, callLog.getCallId().toString())
                    .build();

            kafkaTemplate.send(message);

            log.info("Successfully published call-response-received event for callId: {}, patientId: {}, scheduleId: {}",
                    callLog.getCallId(), callLog.getPatientId(), callLog.getScheduleId());

        } catch (Exception e) {
            log.error("Failed to publish call-response-received event for callId: {}, error: {}",
                    callLog.getCallId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish call response event", e);
        }
    }

    /**
     * Publishes a call-response-received event with additional response status.
     *
     * @param callLog CallLog entity containing call details
     * @param response Response status (TAKEN/NOT_TAKEN/NO_RESPONSE)
     */
    public void publishCallResponseWithStatus(CallLog callLog, String response) {
        try {
            CallResponseEvent event = buildCallResponseEvent(callLog, response);
            
            Message<CallResponseEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, Constants.KAFKA_TOPIC_CALL_RESPONSE)
                    .setHeader(KafkaHeaders.KEY, callLog.getCallId().toString())
                    .build();

            kafkaTemplate.send(message);

            log.info("Successfully published call-response-received event with status '{}' for callId: {}, patientId: {}, scheduleId: {}",
                    response, callLog.getCallId(), callLog.getPatientId(), callLog.getScheduleId());

        } catch (Exception e) {
            log.error("Failed to publish call-response-received event with status '{}' for callId: {}, error: {}",
                    response, callLog.getCallId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish call response event with status", e);
        }
    }

    /**
     * Builds a CallResponseEvent from CallLog entity.
     *
     * @param callLog CallLog entity
     * @param response Optional response status
     * @return CallResponseEvent
     */
    private CallResponseEvent buildCallResponseEvent(CallLog callLog, String response) {
        return CallResponseEvent.builder()
                .callId(callLog.getCallId().toString())
                .patientId(callLog.getPatientId().toString())
                .scheduleId(callLog.getScheduleId().toString())
                .responseReceived(callLog.isResponseReceived())
                .response(response)
                .responseTime(LocalDateTime.now())
                .build();
    }
}'

# ============================================
# FILE 3: Report Service - Kafka Consumer Listener
# ============================================

create_file "report-service/src/main/java/com/medreminder/reportservice/service/EventConsumer.java" 'package com.medreminder.reportservice.service;

import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.dto.DoseMissedEvent;
import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.reportservice.entity.ComplianceData;
import com.medreminder.reportservice.entity.MedicationReport;
import com.medreminder.reportservice.repository.ComplianceDataRepository;
import com.medreminder.reportservice.repository.MedicationReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Kafka Consumer for Report Service.
 * Listens for medication-due, call-response-received, and dose-missed events
 * to build compliance reports.
 */
@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final MedicationReportRepository medicationReportRepository;
    private final ComplianceDataRepository complianceDataRepository;

    @Autowired
    public EventConsumer(MedicationReportRepository medicationReportRepository,
                         ComplianceDataRepository complianceDataRepository) {
        this.medicationReportRepository = medicationReportRepository;
        this.complianceDataRepository = complianceDataRepository;
    }

    /**
     * Handles medication-due events from Kafka.
     * Updates medication report with scheduled medications count.
     *
     * @param event MedicationDueEvent from Kafka
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_MEDICATION_DUE,
        groupId = "report-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMedicationDueEvent(@Payload MedicationDueEvent event) {
        log.info("Received medication-due event: patientId={}, medicationId={}, scheduledTime={}",
                event.getPatientId(), event.getMedicationId(), event.getScheduledTime());

        try {
            UUID patientId = UUID.fromString(event.getPatientId());
            LocalDate reportDate = LocalDate.now();

            // Get or create medication report for today
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, reportDate)
                    .orElseGet(() -> createNewReport(patientId, reportDate));

            // Increment total scheduled medications
            report.setTotalMedicationsScheduled(report.getTotalMedicationsScheduled() + 1);
            updateAdherencePercentage(report);
            
            medicationReportRepository.save(report);

            log.info("Successfully processed medication-due event for patientId: {}, totalScheduled: {}",
                    patientId, report.getTotalMedicationsScheduled());

        } catch (Exception e) {
            log.error("Error processing medication-due event for patientId: {}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Handles call-response-received events from Kafka.
     * Updates medication report with taken medications count.
     *
     * @param event CallResponseEvent from Kafka
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_CALL_RESPONSE,
        groupId = "report-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCallResponseEvent(@Payload CallResponseEvent event) {
        log.info("Received call-response-received event: patientId={}, responseReceived={}",
                event.getPatientId(), event.isResponseReceived());

        try {
            UUID patientId = UUID.fromString(event.getPatientId());
            LocalDate reportDate = LocalDate.now();

            // Get or create medication report for today
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, reportDate)
                    .orElseGet(() -> createNewReport(patientId, reportDate));

            // Update taken count if response was received
            if (event.isResponseReceived()) {
                report.setMedicationsTaken(report.getMedicationsTaken() + 1);
                updateAdherencePercentage(report);
                medicationReportRepository.save(report);

                // Update compliance data for the week/month
                updateComplianceData(patientId, reportDate, true);
            }

            log.info("Successfully processed call-response event for patientId: {}, taken: {}",
                    patientId, event.isResponseReceived());

        } catch (Exception e) {
            log.error("Error processing call-response event for patientId: {}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Handles dose-missed events from Kafka.
     * Updates medication report with missed medications count.
     *
     * @param event DoseMissedEvent from Kafka
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_DOSE_MISSED,
        groupId = "report-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDoseMissedEvent(@Payload DoseMissedEvent event) {
        log.info("Received dose-missed event: patientId={}, scheduleId={}, missedTime={}",
                event.getPatientId(), event.getScheduleId(), event.getMissedTime());

        try {
            UUID patientId = UUID.fromString(event.getPatientId());
            LocalDate reportDate = LocalDate.now();

            // Get or create medication report for today
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, reportDate)
                    .orElseGet(() -> createNewReport(patientId, reportDate));

            // Increment missed medications
            report.setMedicationsMissed(report.getMedicationsMissed() + 1);
            updateAdherencePercentage(report);
            medicationReportRepository.save(report);

            // Update compliance data for the week/month with missed dose
            updateComplianceData(patientId, reportDate, false);

            log.info("Successfully processed dose-missed event for patientId: {}, missed: {}",
                    patientId, report.getMedicationsMissed());

        } catch (Exception e) {
            log.error("Error processing dose-missed event for patientId: {}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Creates a new MedicationReport for a patient for a specific date.
     *
     * @param patientId UUID of patient
     * @param reportDate Date for the report
     * @return New MedicationReport entity
     */
    private MedicationReport createNewReport(UUID patientId, LocalDate reportDate) {
        MedicationReport report = new MedicationReport();
        report.setPatientId(patientId);
        report.setReportDate(reportDate);
        report.setTotalMedicationsScheduled(0);
        report.setMedicationsTaken(0);
        report.setMedicationsMissed(0);
        report.setAdherencePercentage(BigDecimal.ZERO);
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }

    /**
     * Updates compliance data for a patient for the current week and month.
     *
     * @param patientId UUID of patient
     * @param date Date for compliance data
     * @param doseTaken Whether the dose was taken
     */
    private void updateComplianceData(UUID patientId, LocalDate date, boolean doseTaken) {
        try {
            // Update weekly compliance
            int weekNumber = date.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
            String month = date.getMonth().toString();

            Optional<ComplianceData> existingWeekly = complianceDataRepository
                    .findByPatientIdAndWeekNumber(patientId, weekNumber);

            ComplianceData compliance;
            if (existingWeekly.isPresent()) {
                compliance = existingWeekly.get();
            } else {
                compliance = new ComplianceData();
                compliance.setPatientId(patientId);
                compliance.setWeekNumber(weekNumber);
                compliance.setMonth(month);
                compliance.setAdherenceScore(BigDecimal.ZERO);
                compliance.setMissedDoses(0);
                compliance.setEscalationsTriggered(0);
                compliance.setCreatedAt(LocalDateTime.now());
            }

            if (!doseTaken) {
                compliance.setMissedDoses(compliance.getMissedDoses() + 1);
            }

            // Recalculate adherence score
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, date)
                    .orElse(null);

            if (report != null) {
                int total = report.getTotalMedicationsScheduled();
                int taken = report.getMedicationsTaken();
                if (total > 0) {
                    BigDecimal score = BigDecimal.valueOf((double) taken / total * 100)
                            .setScale(2, RoundingMode.HALF_UP);
                    compliance.setAdherenceScore(score);
                }
            }

            complianceDataRepository.save(compliance);
            log.debug("Updated compliance data for patientId: {}, week: {}, month: {}",
                    patientId, weekNumber, month);

        } catch (Exception e) {
            log.error("Error updating compliance data for patientId: {}, error: {}",
                    patientId, e.getMessage(), e);
        }
    }

    /**
     * Updates the adherence percentage for a medication report.
     *
     * @param report MedicationReport to update
     */
    private void updateAdherencePercentage(MedicationReport report) {
        int total = report.getTotalMedicationsScheduled();
        int taken = report.getMedicationsTaken();
        
        if (total > 0) {
            BigDecimal percentage = BigDecimal.valueOf((double) taken / total * 100)
                    .setScale(2, RoundingMode.HALF_UP);
            report.setAdherencePercentage(percentage);
        } else {
            report.setAdherencePercentage(BigDecimal.ZERO);
        }
    }
}'

# ============================================
# FILE 4: Escalation Service - Kafka Consumer Listener
# ============================================

create_file "escalation-service/src/main/java/com/medreminder/escalationservice/service/EscalationEventConsumer.java" 'package com.medreminder.escalationservice.service;

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
            // Don'\''t rethrow - let Kafka commit the offset
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
            // Don'\''t rethrow - escalation is already saved
        }
    }
}'

# ============================================
# FILE 5: Notification Service - Kafka Consumer Listener
# ============================================

create_file "notification-service/src/main/java/com/medreminder/notificationservice/service/NotificationEventConsumer.java" 'package com.medreminder.notificationservice.service;

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
            // For now, we'\''ll send both email and SMS
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
            // Don'\''t rethrow - let Kafka commit the offset
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
}'

# ============================================
# FILE 6: Audit Service - Kafka Consumer Listener
# ============================================

create_file "audit-service/src/main/java/com/medreminder/auditservice/service/AuditEventConsumer.java" 'package com.medreminder.auditservice.service;

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
}'

echo ""
echo "========================================"
echo "All 6 Kafka consumer listener files created successfully!"
echo ""
echo "Created files:"
echo "  Schedule Service:"
echo "    - service/MedicationEventConsumer.java"
echo "  Call Service:"
echo "    - service/CallEventPublisher.java"
echo "  Report Service:"
echo "    - service/EventConsumer.java"
echo "  Escalation Service:"
echo "    - service/EscalationEventConsumer.java"
echo "  Notification Service:"
echo "    - service/NotificationEventConsumer.java"
echo "  Audit Service:"
echo "    - service/AuditEventConsumer.java"
echo ""
echo "Directory structure created:"
echo "  schedule-service/src/main/java/com/medreminder/scheduleservice/service/"
echo "  call-service/src/main/java/com/medreminder/callservice/service/"
echo "  report-service/src/main/java/com/medreminder/reportservice/service/"
echo "  escalation-service/src/main/java/com/medreminder/escalationservice/service/"
echo "  notification-service/src/main/java/com/medreminder/notificationservice/service/"
echo "  audit-service/src/main/java/com/medreminder/auditservice/service/"
echo "========================================"