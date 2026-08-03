package com.medreminder.callservice.service;

import com.medreminder.callservice.entity.CallLog;
import com.medreminder.callservice.exception.ResourceNotFoundException;
import com.medreminder.callservice.repository.CallLogRepository;
import com.medreminder.callservice.grpc.PatientServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Call Service Unit Tests")
class CallServiceTest {

    @Mock
    private CallLogRepository callLogRepository;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CallService callService;

    private CallLog testCallLog;
    private UUID callId;
    private UUID patientId;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        callId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();

        testCallLog = CallLog.builder()
                .callId(callId)
                .patientId(patientId)
                .scheduleId(scheduleId)
                .callInitiatedTime(LocalDateTime.now())
                .callStatus("INITIATED")
                .responseReceived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should initiate call successfully")
    void testInitiateCall_Success() {
        // Arrange
        when(patientServiceClient.getCaregiverPhone(patientId)).thenReturn("+1234567890");
        when(callLogRepository.save(any(CallLog.class))).thenReturn(testCallLog);

        // Act
        CallLog result = callService.initiateCall(patientId, scheduleId, "+1234567890");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCallId()).isEqualTo(callId);
        assertThat(result.getCallStatus()).isEqualTo("INITIATED");
        verify(callLogRepository, times(1)).save(any(CallLog.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid phone number")
    void testInitiateCall_InvalidPhone() {
        // Act & Assert
        assertThatThrownBy(() -> callService.initiateCall(patientId, scheduleId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number cannot be null");
    }

    @Test
    @DisplayName("Should retrieve call log successfully")
    void testGetCallLog_Success() {
        // Arrange
        when(callLogRepository.findById(callId)).thenReturn(Optional.of(testCallLog));

        // Act
        CallLog result = callService.getCallLog(callId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCallId()).isEqualTo(callId);
        verify(callLogRepository, times(1)).findById(callId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when call log not found")
    void testGetCallLog_NotFound() {
        // Arrange
        when(callLogRepository.findById(callId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> callService.getCallLog(callId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Call log not found with ID: " + callId);
    }

    @Test
    @DisplayName("Should update call response successfully")
    void testUpdateCallResponse_Success() {
        // Arrange
        when(callLogRepository.findById(callId)).thenReturn(Optional.of(testCallLog));
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CallLog result = callService.updateCallResponse(callId, true, "TAKEN");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isResponseReceived()).isTrue();
        assertThat(result.getCallStatus()).isEqualTo("COMPLETED");
        verify(callLogRepository, times(1)).findById(callId);
        verify(callLogRepository, times(1)).save(any(CallLog.class));
    }

    @Test
    @DisplayName("Should retry failed call successfully")
    void testRetryCall_Success() {
        // Arrange
        CallLog failedCall = CallLog.builder()
                .callId(callId)
                .patientId(patientId)
                .scheduleId(scheduleId)
                .callStatus("FAILED")
                .responseReceived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(callLogRepository.findById(callId)).thenReturn(Optional.of(failedCall));
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CallLog result = callService.retryCall(callId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCallStatus()).isEqualTo("INITIATED");
        verify(callLogRepository, times(1)).findById(callId);
        verify(callLogRepository, times(1)).save(any(CallLog.class));
    }

    @Test
    @DisplayName("Should record call attempt successfully")
    void testRecordCallAttempt_Success() {
        // Arrange
        when(callLogRepository.findById(callId)).thenReturn(Optional.of(testCallLog));
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CallLog result = callService.recordCallAttempt(callId, 1, "SUCCESS");

        // Assert
        assertThat(result).isNotNull();
        verify(callLogRepository, times(1)).findById(callId);
        verify(callLogRepository, times(1)).save(any(CallLog.class));
    }

    @Test
    @DisplayName("Should retrieve all call logs successfully")
    void testGetAllCallLogs_Success() {
        // Arrange
        List<CallLog> callLogs = Arrays.asList(testCallLog);
        when(callLogRepository.findAll()).thenReturn(callLogs);

        // Act
        List<CallLog> result = callService.getAllCallLogs();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(callLogRepository, times(1)).findAll();
    }
}
