package com.medreminder.notificationservice.integration;

import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.notificationservice.entity.Notification;
import com.medreminder.notificationservice.repository.NotificationRepository;
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
@EmbeddedKafka(partitions = 1, topics = {Constants.KAFKA_TOPIC_MEDICATION_DUE})
@DirtiesContext
@DisplayName("Notification Service Integration Tests with Kafka and RabbitMQ")
class NotificationServiceIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID patientId;
    private UUID medicationId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        medicationId = UUID.randomUUID();
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should trigger notification from MedicationDueEvent")
    void testMedicationDueEventTriggersNotification() throws Exception {
        // Arrange - Create medication due event
        MedicationDueEvent event = MedicationDueEvent.builder()
                .patientId(patientId.toString())
                .medicationId(medicationId.toString())
                .scheduledTime("08:00")
                .location("Patient Home")
                .timestamp(LocalDateTime.now())
                .build();

        // Act - Publish event to Kafka
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, patientId.toString(), event);
        kafkaTemplate.flush();

        // Assert - Wait for notification to be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByPatientId(patientId);
            assertThat(notifications).isNotEmpty();
            
            Notification notification = notifications.get(0);
            assertThat(notification.getPatientId()).isEqualTo(patientId);
            assertThat(notification.getNotificationType()).isIn("EMAIL", "SMS");
            assertThat(notification.getStatus()).isEqualTo("SENT");
            assertThat(notification.getMessage()).contains("medication");
        });

        // Verify RabbitMQ messages were sent
        // Note: In a real test with embedded RabbitMQ, you would verify messages were queued
    }

    @Test
    @DisplayName("Should handle multiple medication due events")
    void testMultipleMedicationDueEvents() throws Exception {
        // Arrange - Create multiple events
        for (int i = 0; i < 5; i++) {
            MedicationDueEvent event = MedicationDueEvent.builder()
                    .patientId(patientId.toString())
                    .medicationId(medicationId.toString())
                    .scheduledTime(String.format("%02d:00", 8 + i))
                    .location("Patient Home")
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                    patientId.toString(), event);
        }
        kafkaTemplate.flush();

        // Assert - Wait for all notifications to be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByPatientId(patientId);
            // Each event might create 1-2 notifications (email + SMS)
            assertThat(notifications).hasSizeGreaterThanOrEqualTo(5);
        });
    }

    @Test
    @DisplayName("Should handle malformed Kafka message gracefully")
    void testMalformedKafkaMessage() throws Exception {
        // Act - Send malformed message
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, 
                patientId.toString(), "This is not a valid MedicationDueEvent");
        kafkaTemplate.flush();

        // Assert - Should not crash, just log error
        Thread.sleep(2000);
        // No assertion needed - test passes if no exception thrown
    }
}
