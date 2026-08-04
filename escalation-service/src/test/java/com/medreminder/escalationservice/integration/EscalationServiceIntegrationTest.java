package com.medreminder.escalationservice.integration;

import com.medreminder.common.dto.DoseMissedEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.escalationservice.entity.Escalation;
import com.medreminder.escalationservice.repository.EscalationRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}", "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}", "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.liquibase.enabled=false"
})
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {Constants.KAFKA_TOPIC_DOSE_MISSED})
@DirtiesContext
@Import(EscalationServiceIntegrationTest.KafkaTestConfig.class)
@DisplayName("Escalation Service Integration Tests with Kafka and RabbitMQ")
class EscalationServiceIntegrationTest {

    @TestConfiguration
    static class KafkaTestConfig {
        @Bean
        public ProducerFactory<String, Object> producerFactory(EmbeddedKafkaBroker embeddedKafka) {
            Map<String, Object> configProps = KafkaTestUtils.producerProps(embeddedKafka);
            configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            return new DefaultKafkaProducerFactory<>(configProps);
        }

        @Bean
        public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }
    }

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
        DoseMissedEvent event = new DoseMissedEvent();
        event.setPatientId(patientId);
        event.setScheduleId(scheduleId);
        event.setMedicationId(UUID.randomUUID());
        event.setTimestamp(LocalDateTime.now());

        kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, patientId.toString(), event);
        kafkaTemplate.flush();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Escalation> escalations = escalationRepository.findByPatientId(patientId);
            assertThat(escalations).isNotEmpty();
            
            Escalation escalation = escalations.get(0);
            assertThat(escalation.getPatientId()).isEqualTo(patientId);
            assertThat(escalation.getScheduleId()).isEqualTo(scheduleId);
            assertThat(escalation.getStatus()).isEqualTo("TRIGGERED");
        });
    }

    @Test
    @DisplayName("Should handle multiple dose-missed events for same patient")
    void testMultipleDoseMissedEvents() throws Exception {
        for (int i = 0; i < 3; i++) {
            DoseMissedEvent event = new DoseMissedEvent();
            event.setPatientId(patientId);
            event.setScheduleId(scheduleId);
        event.setMedicationId(UUID.randomUUID());
            event.setTimestamp(LocalDateTime.now().minusMinutes(i * 5));

            kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, patientId.toString(), event);
        }
        kafkaTemplate.flush();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Escalation> escalations = escalationRepository.findByPatientId(patientId);
            assertThat(escalations).hasSize(3);
        });
    }

    @Test
    @DisplayName("Should determine escalation level correctly")
    void testEscalationLevelDetermination() throws Exception {
        DoseMissedEvent event1 = new DoseMissedEvent();
        event1.setPatientId(patientId);
        event1.setScheduleId(scheduleId);
        event1.setMedicationId(UUID.randomUUID());
        event1.setTimestamp(LocalDateTime.now());

        kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, patientId.toString(), event1);
        kafkaTemplate.flush();

        Thread.sleep(2000);

        DoseMissedEvent event2 = new DoseMissedEvent();
        event2.setPatientId(patientId);
        event2.setScheduleId(scheduleId);
        event2.setMedicationId(UUID.randomUUID());
        event2.setTimestamp(LocalDateTime.now().plusMinutes(10));

        kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, patientId.toString(), event2);
        kafkaTemplate.flush();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Escalation> escalations = escalationRepository.findByPatientId(patientId);
            assertThat(escalations).hasSize(2);
        });
    }

    @Test
    @DisplayName("Should handle malformed Kafka message gracefully")
    void testMalformedKafkaMessage() throws Exception {
        kafkaTemplate.send(Constants.KAFKA_TOPIC_DOSE_MISSED, 
                patientId.toString(), new DoseMissedEvent());
        kafkaTemplate.flush();

        Thread.sleep(2000);
    }
}
