package com.medreminder.reportservice.integration;

import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.dto.DoseMissedEvent;
import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.reportservice.entity.ComplianceData;
import com.medreminder.reportservice.entity.MedicationReport;
import com.medreminder.reportservice.repository.ComplianceDataRepository;
import com.medreminder.reportservice.repository.MedicationReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("Report Service Integration Tests - Multi-Event Kafka Consumption")
class ReportServiceIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private MedicationReportRepository medicationReportRepository;

    @Autowired
    private ComplianceDataRepository complianceDataRepository;

    private UUID patientId;
    private UUID medicationId;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        medicationId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        
        medicationReportRepository.deleteAll();
        complianceDataRepository.deleteAll();
    }

    @Test
    @DisplayName("Should generate compliance report from multiple Kafka events")
    void testComplianceReportGeneration() throws Exception {
        // Arrange - Create 10 medication due events
        for (int i = 0; i < 10; i++) {
            MedicationDueEvent dueEvent = MedicationDueEvent.builder()
                    .patientId(patientId.toString())
                    .medicationId(medicationId.toString())
                    .scheduledTime(String.format("%02d:00", 8 + i))
                    .location("Patient Home")
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                    patientId.toString(), dueEvent);
        }

        // Create 7 call response events (taken)
        for (int i = 0; i < 7; i++) {
            CallResponseEvent responseEvent = CallResponseEvent.builder()
                    .callId(UUID.randomUUID().toString())
                    .patientId(patientId.toString())
                    .scheduleId(scheduleId.toString())
                    .responseReceived(true)
                    .response("TAKEN")
                    .responseTime(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, 
                    patientId.toString(), responseEvent);
        }

        // Create 3 dose missed events
        for (int i = 0; i < 3; i++) {
            DoseMissedEvent missedEvent = DoseMissedEvent.builder()
                    .patientId(patientId.toString())
                    .scheduleId(scheduleId.toString())
                    .missedTime(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, 
                    patientId.toString(), missedEvent);
        }
        kafkaTemplate.flush();

        // Assert - Wait for all events to be processed
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // Check MedicationReport
            LocalDate today = LocalDate.now();
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, today)
                    .orElse(null);

            assertThat(report).isNotNull();
            assertThat(report.getTotalMedicationsScheduled()).isEqualTo(10);
            assertThat(report.getMedicationsTaken()).isEqualTo(7);
            assertThat(report.getMedicationsMissed()).isEqualTo(3);
            
            // Verify adherence percentage
            BigDecimal expected = BigDecimal.valueOf(70.0);
            assertThat(report.getAdherencePercentage())
                    .isEqualByComparingTo(expected);
        });

        // Verify ComplianceData was updated
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ComplianceData> complianceData = complianceDataRepository
                    .findByPatientId(patientId);
            
            assertThat(complianceData).isNotEmpty();
            ComplianceData data = complianceData.get(0);
            assertThat(data.getMissedDoses()).isEqualTo(3);
            assertThat(data.getAdherenceScore())
                    .isEqualByComparingTo(BigDecimal.valueOf(70.0));
        });
    }

    @Test
    @DisplayName("Should handle events with no medication report yet")
    void testEventsWithNoExistingReport() throws Exception {
        // Arrange - Send a call response event before any medication due events
        CallResponseEvent responseEvent = CallResponseEvent.builder()
                .callId(UUID.randomUUID().toString())
                .patientId(patientId.toString())
                .scheduleId(scheduleId.toString())
                .responseReceived(true)
                .response("TAKEN")
                .responseTime(LocalDateTime.now())
                .build();

        // Act
        kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, 
                patientId.toString(), responseEvent);
        kafkaTemplate.flush();

        // Assert - Should create a new MedicationReport
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            LocalDate today = LocalDate.now();
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, today)
                    .orElse(null);
            
            assertThat(report).isNotNull();
            // Should have 1 taken, 0 scheduled, 0 missed
            assertThat(report.getTotalMedicationsScheduled()).isEqualTo(0);
            assertThat(report.getMedicationsTaken()).isEqualTo(1);
            assertThat(report.getMedicationsMissed()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("Should recalculate adherence after each event")
    void testAdherenceRecalculation() throws Exception {
        // Arrange - Send events in sequence
        // 5 medication due events
        for (int i = 0; i < 5; i++) {
            MedicationDueEvent dueEvent = MedicationDueEvent.builder()
                    .patientId(patientId.toString())
                    .medicationId(medicationId.toString())
                    .scheduledTime(String.format("%02d:00", 8 + i))
                    .location("Patient Home")
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                    patientId.toString(), dueEvent);
        }
        kafkaTemplate.flush();

        // Wait a bit, then send 3 taken responses
        Thread.sleep(1000);
        
        for (int i = 0; i < 3; i++) {
            CallResponseEvent responseEvent = CallResponseEvent.builder()
                    .callId(UUID.randomUUID().toString())
                    .patientId(patientId.toString())
                    .scheduleId(scheduleId.toString())
                    .responseReceived(true)
                    .response("TAKEN")
                    .responseTime(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, 
                    patientId.toString(), responseEvent);
        }
        kafkaTemplate.flush();

        // Assert - Final adherence should be 60% (3/5)
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            LocalDate today = LocalDate.now();
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, today)
                    .orElse(null);
            
            assertThat(report).isNotNull();
            assertThat(report.getTotalMedicationsScheduled()).isEqualTo(5);
            assertThat(report.getMedicationsTaken()).isEqualTo(3);
            assertThat(report.getAdherencePercentage())
                    .isEqualByComparingTo(BigDecimal.valueOf(60.0));
        });
    }
}
