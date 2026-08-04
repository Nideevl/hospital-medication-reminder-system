package com.medreminder.scheduleservice.integration;

import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.scheduleservice.entity.Schedule;
import com.medreminder.scheduleservice.repository.ScheduleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {Constants.KAFKA_TOPIC_CALL_RESPONSE, Constants.KAFKA_TOPIC_MEDICATION_DUE}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.group-id=schedule-service-group-test",
    "grpc.server.port=-1",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false"
})
@DisplayName("Schedule Service Integration Tests with Embedded Kafka")
class ScheduleServiceIntegrationTest {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private UUID patientId;
    private UUID medicationId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        medicationId = UUID.randomUUID();

        scheduleRepository.deleteAll();

        kafkaListenerEndpointRegistry.getListenerContainers().forEach(container -> 
            ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic())
        );
    }

    @Test
    void contextLoads() {
        assertThat(scheduleRepository).isNotNull();
    }

    @Test
    @DisplayName("Should consume call-response event and update schedule")
    void testConsumeCallResponseEvent() throws Exception {
        Schedule schedule = Schedule.builder()
                .patientId(patientId)
                .medicationId(medicationId)
                .startDate(LocalDate.now())
                .scheduledTime(LocalTime.of(8, 0))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Schedule savedSchedule = scheduleRepository.saveAndFlush(schedule);
        assertThat(savedSchedule).isNotNull();
        UUID actualScheduleId = savedSchedule.getScheduleId();

        CallResponseEvent event = CallResponseEvent.builder()
                .scheduleId(actualScheduleId)
                .callLogId(UUID.randomUUID())
                .callStatus("COMPLETED")
                .ivrResponse("TAKEN")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, actualScheduleId.toString(), event);
        kafkaTemplate.flush();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Schedule updatedSchedule = scheduleRepository.findById(actualScheduleId).orElse(null);
            assertThat(updatedSchedule).isNotNull();
            assertThat(updatedSchedule.isActive()).isFalse();
        });
    }

    @Test
    @DisplayName("Should handle call-response event when schedule not found gracefully")
    void testConsumeCallResponseEvent_ScheduleNotFound() throws Exception {
        UUID nonExistentScheduleId = UUID.randomUUID();
        CallResponseEvent event = CallResponseEvent.builder()
                .scheduleId(nonExistentScheduleId)
                .callLogId(UUID.randomUUID())
                .callStatus("COMPLETED")
                .ivrResponse("NOT_TAKEN")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, nonExistentScheduleId.toString(), event);
        kafkaTemplate.flush();

        Thread.sleep(2000);
    }
}
