package com.medreminder.callservice.service;

import com.medreminder.callservice.dto.CallLogResponse;
import com.medreminder.callservice.dto.InitiateCallRequest;
import com.medreminder.callservice.dto.UpdateCallResponseRequest;
import com.medreminder.callservice.entity.CallAttempt;
import com.medreminder.callservice.entity.CallLog;
import com.medreminder.callservice.exception.CallNotFoundException;
import com.medreminder.callservice.repository.CallAttemptRepository;
import com.medreminder.callservice.repository.CallLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CallService Unit Tests")
class CallServiceTest {

    @Mock
    private CallLogRepository callLogRepository;

    @Mock
    private CallAttemptRepository callAttemptRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CallService callService;

    private UUID callId;
    private UUID patientId;
    private UUID scheduleId;
    private CallLog callLog;

    @BeforeEach
    void setUp() {
        callId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();

        callLog = new CallLog();
        callLog.setCallId(callId);
        callLog.setPatientId(patientId);
        callLog.setScheduleId(scheduleId);
        callLog.setCallStatus("INITIATED");
        callLog.setCallInitiatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should initiate call successfully")
    void testInitiateCall_Success() {
        InitiateCallRequest request = new InitiateCallRequest();
        request.setPatientId(patientId);
        request.setScheduleId(scheduleId);

        when(callLogRepository.save(any(CallLog.class))).thenReturn(callLog);

        CallLogResponse response = callService.initiateCall(request);

        assertThat(response).isNotNull();
        assertThat(response.getCallId()).isEqualTo(callId);
        assertThat(response.getPatientId()).isEqualTo(patientId);
        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getCallStatus()).isEqualTo("INITIATED");

        verify(callLogRepository, times(1)).save(any(CallLog.class));
        verify(callAttemptRepository, times(1)).save(any(CallAttempt.class));
    }

    @Test
    @DisplayName("Should update call response successfully")
    void testUpdateCallResponse_Success() {
        UpdateCallResponseRequest request = new UpdateCallResponseRequest();
        request.setCallId(callId);
        request.setCallStatus("COMPLETED");
        request.setIvrResponse("1");
        request.setCallDurationSeconds(30);

        when(callLogRepository.findById(callId)).thenReturn(Optional.of(callLog));
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CallLogResponse response = callService.updateCallResponse(callId, request);

        assertThat(response).isNotNull();
        assertThat(response.getCallStatus()).isEqualTo("COMPLETED");
        assertThat(response.getIvrResponse()).isEqualTo("1");
        assertThat(response.getCallDurationSeconds()).isEqualTo(30);

        verify(callLogRepository, times(1)).findById(callId);
        verify(callLogRepository, times(1)).save(any(CallLog.class));
    }

    @Test
    @DisplayName("Should throw CallNotFoundException when updating non-existent call")
    void testUpdateCallResponse_NotFound() {
        UpdateCallResponseRequest request = new UpdateCallResponseRequest();
        request.setCallId(callId);

        when(callLogRepository.findById(callId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> callService.updateCallResponse(callId, request))
                .isInstanceOf(CallNotFoundException.class)
                .hasMessageContaining("Call log not found");
    }

    @Test
    @DisplayName("Should retrieve call log by ID")
    void testGetCallLog_Success() {
        when(callLogRepository.findById(callId)).thenReturn(Optional.of(callLog));

        CallLogResponse response = callService.getCallLog(callId);

        assertThat(response).isNotNull();
        assertThat(response.getCallId()).isEqualTo(callId);
    }

    @Test
    @DisplayName("Should retrieve patient call history")
    void testGetPatientCallHistory() {
        when(callLogRepository.findByPatientId(patientId)).thenReturn(List.of(callLog));

        List<CallLogResponse> history = callService.getPatientCallHistory(patientId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getPatientId()).isEqualTo(patientId);
    }
}
