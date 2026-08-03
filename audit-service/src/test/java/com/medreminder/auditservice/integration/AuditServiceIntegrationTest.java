package com.medreminder.auditservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medreminder.auditservice.entity.MedicationAudit;
import com.medreminder.auditservice.repository.MedicationAuditRepository;
import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.dto.DoseMissedEvent;
import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {
        Constants.KAFKA_TOPIC_MEDICATION_DUE,
        Constants.KAFKA_TOPIC_CALL_RESPONSE,
        Constants.KAFKA_TOPIC_DOSE_MISSED
})
@DirtiesContext
@DisplayName("Audit Service Integration Tests")
class AuditServiceIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicationAuditRepository medicationAuditRepository;

    private UUID patientId;
    private UUID medicationId;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        medicationId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        medicationAuditRepository.deleteAll();
    }

    @Test
    @DisplayName("Should audit all event types")
    void testAuditTrailForAllEvents() throws Exception {
        // 1. Send MedicationDueEvent
        MedicationDueEvent dueEvent = MedicationDueEvent.builder()
                .patientId(patientId.toString())
                .medicationId(medicationId.toString())
                .scheduledTime("08:00")
                .location("Patient Home")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), dueEvent);
        kafkaTemplate.flush();

        // 2. Send CallResponseEvent
        CallResponseEvent callEvent = CallResponseEvent.builder()
                .callId(UUID.randomUUID().toString())
                .patientId(patientId.toString())
                .scheduleId(scheduleId.toString())
                .responseReceived(true)
                .response("TAKEN")
                .responseTime(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, 
                patientId.toString(), callEvent);
        kafkaTemplate.flush();

        // 3. Send DoseMissedEvent
        DoseMissedEvent doseEvent = DoseMissedEvent.builder()
                .patientId(patientId.toString())
                .scheduleId(scheduleId.toString())
                .missedTime(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, 
                patientId.toString(), doseEvent);
        kafkaTemplate.flush();

        // Assert - Wait for all audits to be created
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MedicationAudit> audits = medicationAuditRepository
                    .findByPatientId(patientId);
            
            assertThat(audits).hasSize(3);
            
            // Verify action types
            List<String> actionTypes = audits.stream()
                    .map(MedicationAudit::getActionType)
                    .collect(java.util.stream.Collectors.toList());
            
            assertThat(actionTypes).containsExactlyInAnyOrder(
                    "MEDICATION_DUE",
                    "CALL_RESPONSE_TAKEN",
                    "DOSE_MISSED"
            );
        });
    }

    @Test
    @DisplayName("Should include full event payload in audit details")
    void testAuditDetailsContainFullPayload() throws Exception {
        // Arrange
        MedicationDueEvent event = MedicationDueEvent.builder()
                .patientId(patientId.toString())
                .medicationId(medicationId.toString())
                .scheduledTime("09:30")
                .location("Workplace")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), event);
        kafkaTemplate.flush();

        // Assert
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MedicationAudit> audits = medicationAuditRepository
                    .findByPatientId(patientId);
            
            assertThat(audits).isNotEmpty();
            
            MedicationAudit audit = audits.get(0);
            assertThat(audit.getActionDetails()).isNotNull();
            assertThat(audit.getActionDetails()).contains(patientId.toString());
            assertThat(audit.getActionDetails()).contains(medicationId.toString());
            assertThat(audit.getActionDetails()).contains("09:30");
        });
    }

    @Test
    @DisplayName("Should handle duplicate events gracefully")
    void testDuplicateEvents() throws Exception {
        // Arrange - Send same event twice
        MedicationDueEvent event = MedicationDueEvent.builder()
                .patientId(patientId.toString())
                .medicationId(medicationId.toString())
                .scheduledTime("08:00")
                .location("Patient Home")
                .timestamp(LocalDateTime.now())
                .build();

        // Act - Send duplicate events
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), event);
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), event);
        kafkaTemplate.flush();

        // Assert - Both events should be audited
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MedicationAudit> audits = medicationAuditRepository
                    .findByPatientId(patientId);
            
            assertThat(audits).hasSize(2);
            
            // Verify both have same details
            MedicationAudit audit1 = audits.get(0);
            MedicationAudit audit2 = audits.get(1);
            
            assertThat(audit1.getActionDetails())
                    .isEqualTo(audit2.getActionDetails());
        });
    }

    @Test
    @DisplayName("Should audit multiple events for different patients")
    void testMultiplePatientsAudit() throws Exception {
        // Arrange
        UUID patientId2 = UUID.randomUUID();
        UUID medicationId2 = UUID.randomUUID();

        // Send events for patient 1
        MedicationDueEvent event1 = MedicationDueEvent.builder()
                .patientId(patientId.toString())
                .medicationId(medicationId.toString())
                .scheduledTime("08:00")
                .location("Home")
                .timestamp(LocalDateTime.now())
                .build();

        // Send events for patient 2
        MedicationDueEvent event2 = MedicationDueEvent.builder()
                .patientId(patientId2.toString())
                .medicationId(medicationId2.toString())
                .scheduledTime("09:00")
                .location("Office")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), event1);
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId2.toString(), event2);
        kafkaTemplate.flush();

        // Assert
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MedicationAudit> audits1 = medicationAuditRepository
                    .findByPatientId(patientId);
            List<MedicationAudit> audits2 = medicationAuditRepository
                    .findByPatientId(patientId2);
            
            assertThat(audits1).hasSize(1);
            assertThat(audits2).hasSize(1);
            
            assertThat(audits1.get(0).getPatientId()).isEqualTo(patientId);
            assertThat(audits2.get(0).getPatientId()).isEqualTo(patientId2);
        });
    }
}
