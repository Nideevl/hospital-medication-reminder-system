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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
    "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
    "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
    "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.properties.spring.json.trusted.packages=*"
})
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
        MedicationReport initialReport = new MedicationReport();
        initialReport.setPatientId(patientId);
        initialReport.setReportDate(LocalDate.now());
        initialReport.setTotalMedicationsScheduled(0);
        initialReport.setMedicationsTaken(0);
        initialReport.setMedicationsMissed(0);
        initialReport.setAdherencePercentage(0.0);
        initialReport.setCreatedAt(LocalDateTime.now());
        medicationReportRepository.save(initialReport);

        for (int i = 0; i < 10; i++) {
            final int expected = i + 1;
            MedicationDueEvent dueEvent = MedicationDueEvent.builder()
                    .patientId(patientId)
                    .medicationId(medicationId)
                    .scheduledTime(LocalTime.of(8, 0))
                    .build();
            kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE,
                    patientId.toString(), dueEvent).get(5, TimeUnit.SECONDS);

            await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
                MedicationReport report = medicationReportRepository
                        .findByPatientIdAndReportDate(patientId, LocalDate.now())
                        .orElse(null);
                assertThat(report).isNotNull();
                assertThat(report.getTotalMedicationsScheduled()).isEqualTo(expected);
            });
        }

        for (int i = 0; i < 7; i++) {
            final int expected = i + 1;
            CallResponseEvent responseEvent = CallResponseEvent.builder()
                    .callLogId(UUID.randomUUID())
                    .patientId(patientId)
                    .scheduleId(scheduleId)
                    .ivrResponse("TAKEN")
                    .responseReceived(true)
                    .callStatus("COMPLETED")
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE,
                    patientId.toString(), responseEvent).get(5, TimeUnit.SECONDS);

            await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
                MedicationReport report = medicationReportRepository
                        .findByPatientIdAndReportDate(patientId, LocalDate.now())
                        .orElse(null);
                assertThat(report).isNotNull();
                assertThat(report.getMedicationsTaken()).isEqualTo(expected);
            });
        }

        for (int i = 0; i < 3; i++) {
            final int expected = i + 1;
            DoseMissedEvent missedEvent = DoseMissedEvent.builder()
                    .patientId(patientId)
                    .scheduleId(scheduleId)
                    .missedTime(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED,
                    patientId.toString(), missedEvent).get(5, TimeUnit.SECONDS);

            await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
                MedicationReport report = medicationReportRepository
                        .findByPatientIdAndReportDate(patientId, LocalDate.now())
                        .orElse(null);
                assertThat(report).isNotNull();
                assertThat(report.getMedicationsMissed()).isEqualTo(expected);
            });
        }

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ComplianceData> result = complianceDataRepository
                    .findByPatientIdOrderByCreatedAtDesc(patientId);

            assertThat(result).isNotEmpty();
            ComplianceData data = result.get(0);
            assertThat(data.getMissedDoses()).isEqualTo(3);
            assertThat(data.getAdherenceScore()).isEqualTo(70.0);
        });
    }

    @Test
    @DisplayName("Should handle events with no medication report yet")
    void testEventsWithNoExistingReport() throws Exception {
        CallResponseEvent responseEvent = CallResponseEvent.builder()
                .callLogId(UUID.randomUUID())
                .patientId(patientId)
                .scheduleId(scheduleId)
                .ivrResponse("TAKEN")
                .responseReceived(true)
                .callStatus("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE,
                patientId.toString(), responseEvent).get(5, TimeUnit.SECONDS);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            LocalDate today = LocalDate.now();
            List<MedicationReport> reports = medicationReportRepository.findByPatientId(patientId)
                    .stream()
                    .filter(r -> today.equals(r.getReportDate()))
                    .toList();

            assertThat(reports).isNotEmpty();

            int totalTaken = reports.stream().mapToInt(MedicationReport::getMedicationsTaken).sum();
            assertThat(totalTaken).isEqualTo(1);
        });
    }
}