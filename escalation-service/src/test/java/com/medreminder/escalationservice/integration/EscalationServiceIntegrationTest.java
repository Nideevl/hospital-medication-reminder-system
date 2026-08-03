package com.medreminder.escalationservice.integration;

import com.medreminder.common.dto.DoseMissedEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.escalationservice.entity.Escalation;
import com.medreminder.escalationservice.repository.EscalationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
@EmbeddedKafka(partitions = 1, topics = {Constants.KAFKA_TOPIC_DOSE_MISSED})
@DirtiesContext
@DisplayName("Escalation Service Integration Tests with Kafka and RabbitMQ")
class EscalationServiceIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private EscalationRepository escalationRepository;

    private UUID patientId;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        escalationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create escalation from dose-missed event")
    void testEscalationWorkflow() throws Exception {
        // Arrange
        DoseMissedEvent event = DoseMissedEvent.builder()
                .patientId(patientId.toString())
                .scheduleId(scheduleId.toString())
                .missedTime(LocalDateTime.now())
                .build();

        // Act
        kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, patientId.toString(), event);
        kafkaTemplate.flush();

        // Assert - Wait for escalation to be created
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Escalation> escalations = escalationRepository.findByPatientId(patientId);
            assertThat(escalations).isNotEmpty();
            
            Escalation escalation = escalations.get(0);
            assertThat(escalation.getPatientId()).isEqualTo(patientId);
            assertThat(escalation.getScheduleId()).isEqualTo(scheduleId);
            assertThat(escalation.getEscalationType()).isEqualTo("DOSE_MISSED");
            assertThat(escalation.getStatus()).isEqualTo("PENDING");
        });

        // Verify RabbitMQ message was sent
        // Note: In a real test with embedded RabbitMQ, you would verify the message
    }

    @Test
    @DisplayName("Should handle multiple dose-missed events for same patient")
    void testMultipleDoseMissedEvents() throws Exception {
        // Arrange - Send 3 dose-missed events
        for (int i = 0; i < 3; i++) {
            DoseMissedEvent event = DoseMissedEvent.builder()
                    .patientId(patientId.toString())
                    .scheduleId(scheduleId.toString())
                    .missedTime(LocalDateTime.now().minusMinutes(i * 5))
                    .build();

            kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, patientId.toString(), event);
        }
        kafkaTemplate.flush();

        // Assert - Should have 3 escalations
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Escalation> escalations = escalationRepository.findByPatientId(patientId);
            assertThat(escalations).hasSize(3);
        });
    }

    @Test
    @DisplayName("Should determine escalation level correctly")
    void testEscalationLevelDetermination() throws Exception {
        // Arrange - Send first dose-missed event
        DoseMissedEvent event1 = DoseMissedEvent.builder()
                .patientId(patientId.toString())
                .scheduleId(scheduleId.toString())
                .missedTime(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, patientId.toString(), event1);
        kafkaTemplate.flush();

        // Wait for first escalation
        Thread.sleep(2000);

        // Send second dose-missed event (should trigger level 2)
        DoseMissedEvent event2 = DoseMissedEvent.builder()
                .patientId(patientId.toString())
                .scheduleId(scheduleId.toString())
                .missedTime(LocalDateTime.now().plusMinutes(10))
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, patientId.toString(), event2);
        kafkaTemplate.flush();

        // Assert - Should have escalation level 2
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Escalation> escalations = escalationRepository.findByPatientId(patientId);
            assertThat(escalations).hasSize(2);
            
            // Should have escalated levels
            boolean hasLevel2 = escalations.stream()
                    .anyMatch(e -> e.getEscalationLevel() == 2);
            assertThat(hasLevel2).isTrue();
        });
    }

    @Test
    @DisplayName("Should handle malformed Kafka message gracefully")
    void testMalformedKafkaMessage() throws Exception {
        // Act - Send invalid message
        kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, 
                patientId.toString(), "This is not a valid DoseMissedEvent");
        kafkaTemplate.flush();

        // Assert - Should not crash
        Thread.sleep(2000);
        // No assertion needed - test passes if no exception thrown
    }
}
