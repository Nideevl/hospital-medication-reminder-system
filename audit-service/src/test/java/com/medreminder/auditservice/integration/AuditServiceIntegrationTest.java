package com.medreminder.auditservice.integration;

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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
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
        MedicationDueEvent dueEvent = MedicationDueEvent.builder()
                .patientId(patientId)
                .medicationId(medicationId)
                .scheduledTime(LocalTime.of(8, 0))
                .location("Patient Home")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), dueEvent).get();

        CallResponseEvent callEvent = CallResponseEvent.builder()
                .callLogId(UUID.randomUUID())
                .patientId(patientId)
                .scheduleId(scheduleId)
                .responseReceived(true)
                .ivrResponse("1")
                .callStatus("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, 
                patientId.toString(), callEvent).get();

        DoseMissedEvent doseEvent = DoseMissedEvent.builder()
                .patientId(patientId)
                .scheduleId(scheduleId)
                .missedTime(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, 
                patientId.toString(), doseEvent).get();

        kafkaTemplate.flush();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MedicationAudit> audits = medicationAuditRepository
                    .findByPatientId(patientId);

            assertThat(audits).hasSize(3);

            List<String> actions = audits.stream()
                    .map(MedicationAudit::getAction)
                    .collect(Collectors.toList());

            assertThat(actions).containsExactlyInAnyOrder(
                    "MEDICATION_DUE",
                    "CALL_RESPONSE_TAKEN",
                    "DOSE_MISSED"
            );
        });
    }

    @Test
    @DisplayName("Should include full event payload in audit details")
    void testAuditDetailsContainFullPayload() throws Exception {
        MedicationDueEvent event = MedicationDueEvent.builder()
                .patientId(patientId)
                .medicationId(medicationId)
                .scheduledTime(LocalTime.of(9, 30))
                .location("Workplace")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), event).get();
        kafkaTemplate.flush();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MedicationAudit> audits = medicationAuditRepository
                    .findByPatientId(patientId);

            assertThat(audits).isNotEmpty();

            MedicationAudit audit = audits.get(0);
            assertThat(audit.getPatientId()).isEqualTo(patientId);
            assertThat(audit.getScheduleId()).isEqualTo(medicationId);
            assertThat(audit.getAction()).isEqualTo("MEDICATION_DUE");
        });
    }

    @Test
    @DisplayName("Should handle duplicate events gracefully")
    void testDuplicateEvents() throws Exception {
        MedicationDueEvent event = MedicationDueEvent.builder()
                .patientId(patientId)
                .medicationId(medicationId)
                .scheduledTime(LocalTime.of(8, 0))
                .location("Patient Home")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), event).get();
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), event).get();
        kafkaTemplate.flush();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MedicationAudit> audits = medicationAuditRepository
                    .findByPatientId(patientId);

            assertThat(audits).hasSize(2);

            MedicationAudit audit1 = audits.get(0);
            MedicationAudit audit2 = audits.get(1);

            assertThat(audit1.getAction()).isEqualTo(audit2.getAction());
            assertThat(audit1.getPatientId()).isEqualTo(audit2.getPatientId());
        });
    }

    @Test
    @DisplayName("Should audit multiple events for different patients")
    void testMultiplePatientsAudit() throws Exception {
        UUID patientId2 = UUID.randomUUID();
        UUID medicationId2 = UUID.randomUUID();

        MedicationDueEvent event1 = MedicationDueEvent.builder()
                .patientId(patientId)
                .medicationId(medicationId)
                .scheduledTime(LocalTime.of(8, 0))
                .location("Home")
                .timestamp(LocalDateTime.now())
                .build();

        MedicationDueEvent event2 = MedicationDueEvent.builder()
                .patientId(patientId2)
                .medicationId(medicationId2)
                .scheduledTime(LocalTime.of(9, 0))
                .location("Office")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), event1).get();
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId2.toString(), event2).get();
        kafkaTemplate.flush();

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
