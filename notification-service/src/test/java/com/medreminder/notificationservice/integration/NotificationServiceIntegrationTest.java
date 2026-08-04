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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {Constants.KAFKA_TOPIC_MEDICATION_DUE},
    controlledShutdown = true
)
@TestPropertySource(properties = {
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
    "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.group-id=notification-group-test",
    "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
@DirtiesContext
@DisplayName("Notification Service Integration Tests with Kafka and RabbitMQ")
class NotificationServiceIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
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
    @DisplayName("Should process medication due event and create notification record")
    void testMedicationDueEventTriggersNotification() {
        MedicationDueEvent event = MedicationDueEvent.builder()
                .patientId(patientId)
                .medicationId(medicationId)
                .scheduledTime(LocalTime.of(8, 0))
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, patientId.toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByPatientId(patientId);
            assertThat(notifications).isNotEmpty();
            assertThat(notifications.get(0).getPatientId()).isEqualTo(patientId);
        });
    }

    @Test
    @DisplayName("Should handle multiple medication due events correctly")
    void testMultipleMedicationDueEvents() {
        for (int i = 0; i < 3; i++) {
            MedicationDueEvent event = MedicationDueEvent.builder()
                    .patientId(patientId)
                    .medicationId(medicationId)
                    .scheduledTime(LocalTime.of(8 + i, 0))
                    .build();

            kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, patientId.toString(), event);
        }

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByPatientId(patientId);
            assertThat(notifications).hasSizeGreaterThanOrEqualTo(3);
        });
    }

    @Test
    @DisplayName("Should safely handle malformed kafka messages without breaking context")
    void testMalformedKafkaMessage() {
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, patientId.toString(), "INVALID_PAYLOAD");

        await().pollDelay(2, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByPatientId(patientId);
            assertThat(notifications).isEmpty();
        });
    }
}
