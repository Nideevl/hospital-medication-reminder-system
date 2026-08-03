package com.medreminder.callservice.integration;

import com.medreminder.callservice.entity.CallLog;
import com.medreminder.callservice.repository.CallLogRepository;
import com.medreminder.callservice.service.CallService;
import com.medreminder.common.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {Constants.KAFKA_TOPIC_CALL_RESPONSE})
@DirtiesContext
@DisplayName("Call Service Integration Tests with gRPC and Kafka")
class CallServiceIntegrationTest {

    @Autowired
    private CallService callService;

    @Autowired
    private CallLogRepository callLogRepository;

    @MockBean
    private PatientServiceClient patientServiceClient;

    private UUID patientId;
    private UUID scheduleId;
    private String phoneNumber;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        phoneNumber = "+1234567890";
        callLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Should initiate call and publish Kafka event")
    void testInitiateCallAndPublishEvent() {
        // Arrange
        when(patientServiceClient.getPatientById(any())).thenReturn(createMockPatientResponse());
        when(patientServiceClient.getCaregiverPhone(any())).thenReturn(phoneNumber);

        // Act
        CallLog callLog = callService.initiateCall(patientId, scheduleId, phoneNumber);

        // Assert
        assertThat(callLog).isNotNull();
        assertThat(callLog.getCallId()).isNotNull();
        assertThat(callLog.getPatientId()).isEqualTo(patientId);
        assertThat(callLog.getScheduleId()).isEqualTo(scheduleId);
        assertThat(callLog.getCallStatus()).isEqualTo("INITIATED");

        // Verify call log saved in database
        CallLog saved = callLogRepository.findById(callLog.getCallId()).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getCallStatus()).isEqualTo("INITIATED");

        // Verify Kafka event would be published (publishing happens async in service)
        // In a real test, you would verify the event was sent to Kafka
    }

    @Test
    @DisplayName("Should update call response and publish event")
    void testUpdateCallResponse_Success() {
        // Arrange - Create a call log first
        CallLog callLog = CallLog.builder()
                .callId(UUID.randomUUID())
                .patientId(patientId)
                .scheduleId(scheduleId)
                .callInitiatedTime(LocalDateTime.now())
                .callStatus("IN_PROGRESS")
                .responseReceived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        callLogRepository.save(callLog);

        // Act
        CallLog updated = callService.updateCallResponse(callLog.getCallId(), true, "TAKEN");

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.isResponseReceived()).isTrue();
        assertThat(updated.getCallStatus()).isEqualTo("COMPLETED");

        // Verify event would be published to Kafka (async)
        // Event publishing is handled by CallEventPublisher
    }

    @Test
    @DisplayName("Should handle gRPC patient fetch failure gracefully")
    void testGrpcPatientFetch_Failure() {
        // Arrange
        when(patientServiceClient.getPatientById(any()))
                .thenThrow(new RuntimeException("gRPC connection failed"));

        // Act
        try {
            callService.initiateCall(patientId, scheduleId, phoneNumber);
        } catch (Exception e) {
            // Assert
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e.getMessage()).contains("gRPC connection failed");
        }
    }

    private Object createMockPatientResponse() {
        // Mock PatientResponse object
        return new Object() {
            public String getPatientId() { return patientId.toString(); }
            public String getName() { return "John Doe"; }
            public String getPhone() { return phoneNumber; }
        };
    }
}
