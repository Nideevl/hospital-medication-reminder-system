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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {Constants.KAFKA_TOPIC_CALL_RESPONSE})
@DirtiesContext
@DisplayName("Schedule Service Integration Tests with Embedded Kafka")
class ScheduleServiceIntegrationTest {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private UUID scheduleId;
    private UUID patientId;
    private UUID medicationId;

    @BeforeEach
    void setUp() {
        scheduleId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        medicationId = UUID.randomUUID();

        // Clean up any existing data
        scheduleRepository.deleteAll();
    }

    @Test
    @DisplayName("Should consume call-response event and update schedule")
    void testConsumeCallResponseEvent() throws Exception {
        // Arrange - Create a schedule
        Schedule schedule = Schedule.builder()
                .scheduleId(scheduleId)
                .patientId(patientId)
                .medicationId(medicationId)
                .scheduledTime(LocalTime.of(8, 0))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        scheduleRepository.save(schedule);

        // Create and publish call response event
        CallResponseEvent event = CallResponseEvent.builder()
                .callId(UUID.randomUUID().toString())
                .patientId(patientId.toString())
                .scheduleId(scheduleId.toString())
                .responseReceived(true)
                .response("TAKEN")
                .responseTime(LocalDateTime.now())
                .build();

        // Act - Publish event to Kafka
        kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, scheduleId.toString(), event);
        kafkaTemplate.flush();

        // Assert - Wait for consumer to process
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Schedule updatedSchedule = scheduleRepository.findById(scheduleId).orElse(null);
            assertThat(updatedSchedule).isNotNull();
            // Schedule should be deactivated when medication is taken
            assertThat(updatedSchedule.isActive()).isFalse();
        });
    }

    @Test
    @DisplayName("Should handle call-response event when schedule not found gracefully")
    void testConsumeCallResponseEvent_ScheduleNotFound() throws Exception {
        // Arrange - Create event for non-existent schedule
        UUID nonExistentScheduleId = UUID.randomUUID();
        CallResponseEvent event = CallResponseEvent.builder()
                .callId(UUID.randomUUID().toString())
                .patientId(patientId.toString())
                .scheduleId(nonExistentScheduleId.toString())
                .responseReceived(false)
                .response("NOT_TAKEN")
                .responseTime(LocalDateTime.now())
                .build();

        // Act - Publish event to Kafka
        kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, nonExistentScheduleId.toString(), event);
        kafkaTemplate.flush();

        // Assert - Should not throw exception, just log error
        // Wait a bit to ensure no errors occur
        Thread.sleep(2000);
        // No assertion needed - test passes if no exception thrown
    }
}
