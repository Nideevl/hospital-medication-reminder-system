package com.medreminder.callservice.integration;

import com.medreminder.callservice.dto.CallLogResponse;
import com.medreminder.callservice.dto.InitiateCallRequest;
import org.springframework.test.context.TestPropertySource;
import com.medreminder.callservice.dto.UpdateCallResponseRequest;
import com.medreminder.callservice.entity.CallLog;
import com.medreminder.callservice.repository.CallLogRepository;
import com.medreminder.callservice.service.CallService;
import com.medreminder.common.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {Constants.KAFKA_TOPIC_CALL_RESPONSE})
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.liquibase.enabled=false", "spring.liquibase.duplicate-file-mode=WARN"
})
@DisplayName("Call Service Integration Tests with Embedded Kafka")
class CallServiceIntegrationTest {

    @Autowired
    private CallService callService;

    @Autowired
    private CallLogRepository callLogRepository;

    private UUID patientId;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        callLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Should initiate call and record attempt in database")
    void testInitiateCall_Success() {
        // Arrange
        InitiateCallRequest request = new InitiateCallRequest();
        request.setPatientId(patientId);
        request.setScheduleId(scheduleId);

        // Act
        CallLogResponse response = callService.initiateCall(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCallId()).isNotNull();
        assertThat(response.getPatientId()).isEqualTo(patientId);
        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getCallStatus()).isEqualTo("INITIATED");

        // Verify call log saved in database
        CallLog saved = callLogRepository.findById(response.getCallId()).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getCallStatus()).isEqualTo("INITIATED");
    }

    @Test
    @DisplayName("Should update call response and persist state")
    void testUpdateCallResponse_Success() {
        // Arrange - Create and save initial call log
        CallLog callLog = new CallLog();
        callLog.setPatientId(patientId);
        callLog.setScheduleId(scheduleId);
        callLog.setCallStatus("INITIATED");
        callLog.setCallInitiatedAt(LocalDateTime.now());
        CallLog savedCallLog = callLogRepository.save(callLog);

        UpdateCallResponseRequest request = new UpdateCallResponseRequest();
        request.setCallId(savedCallLog.getCallId());
        request.setCallStatus("COMPLETED");
        request.setIvrResponse("1");
        request.setCallDurationSeconds(45);

        // Act
        CallLogResponse updated = callService.updateCallResponse(savedCallLog.getCallId(), request);

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.getCallStatus()).isEqualTo("COMPLETED");
        assertThat(updated.getIvrResponse()).isEqualTo("1");
        assertThat(updated.getCallDurationSeconds()).isEqualTo(45);

        // Verify database persistence
        CallLog persisted = callLogRepository.findById(savedCallLog.getCallId()).orElse(null);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getCallStatus()).isEqualTo("COMPLETED");
        assertThat(persisted.getIvrResponse()).isEqualTo("1");
    }
}
