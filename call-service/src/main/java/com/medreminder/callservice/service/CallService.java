package com.medreminder.callservice.service;

import com.medreminder.callservice.dto.CallLogResponse;
import com.medreminder.callservice.dto.InitiateCallRequest;
import com.medreminder.callservice.dto.UpdateCallResponseRequest;
import com.medreminder.callservice.entity.CallAttempt;
import com.medreminder.callservice.entity.CallLog;
import com.medreminder.callservice.exception.CallInitiationException;
import com.medreminder.callservice.exception.CallNotFoundException;
import com.medreminder.callservice.repository.CallAttemptRepository;
import com.medreminder.callservice.repository.CallLogRepository;
import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CallService {

    private static final Logger log = LoggerFactory.getLogger(CallService.class);

    private final CallLogRepository callLogRepository;
    private final CallAttemptRepository callAttemptRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public CallService(CallLogRepository callLogRepository,
                       CallAttemptRepository callAttemptRepository,
                       KafkaTemplate<String, Object> kafkaTemplate) {
        this.callLogRepository = callLogRepository;
        this.callAttemptRepository = callAttemptRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public CallLogResponse initiateCall(InitiateCallRequest request) {
        log.info("Initiating call for patient: {}, schedule: {}", request.getPatientId(), request.getScheduleId());

        try {
            CallLog callLog = new CallLog();
            callLog.setPatientId(request.getPatientId());
            callLog.setScheduleId(request.getScheduleId());
            callLog.setCallStatus("INITIATED");

            CallLog savedCallLog = callLogRepository.save(callLog);

            CallAttempt attempt = new CallAttempt();
            attempt.setCallId(savedCallLog.getCallId());
            attempt.setAttemptNumber(1);
            attempt.setStatus("SUCCESS");
            attempt.setAttemptedAt(LocalDateTime.now());

            callAttemptRepository.save(attempt);

            return mapToResponse(savedCallLog);
        } catch (Exception e) {
            log.error("Failed to initiate call for patient: {}", request.getPatientId(), e);
            throw new CallInitiationException("Failed to initiate call: " + e.getMessage());
        }
    }

    public CallLogResponse updateCallResponse(UUID callId, UpdateCallResponseRequest request) {
        log.info("Updating call response for callId: {}", callId);

        CallLog callLog = callLogRepository.findById(callId)
                .orElseThrow(() -> new CallNotFoundException("Call log not found with id:" + callId));

        callLog.setCallStatus(request.getCallStatus());
        callLog.setIvrResponse(request.getIvrResponse());
        callLog.setCallDurationSeconds(request.getCallDurationSeconds());
        callLog.setCallAnsweredAt(LocalDateTime.now());

        CallLog updatedCallLog = callLogRepository.save(callLog);

        try {
            CallResponseEvent event = CallResponseEvent.builder()
                    .callLogId(updatedCallLog.getCallId())
                    .scheduleId(updatedCallLog.getScheduleId())
                    .callStatus(updatedCallLog.getCallStatus())
                    .ivrResponse(request.getIvrResponse())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, updatedCallLog.getScheduleId().toString(), event);
            log.info("Published call response event to Kafka");
        } catch (Exception e) {
            log.warn("Failed to publish call response event: {}", e.getMessage());
        }

        return mapToResponse(updatedCallLog);
    }

    public CallLogResponse getCallLog(UUID callId) {
        CallLog callLog = callLogRepository.findById(callId)
                .orElseThrow(() -> new CallNotFoundException("Call log not found with id: " + callId));
        return mapToResponse(callLog);
    }

    public List<CallLogResponse> getPatientCallHistory(UUID patientId) {
        return callLogRepository.findByPatientId(patientId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CallAttempt> getCallAttempts(UUID callId) {
        return callAttemptRepository.findByCallId(callId);
    }

    private CallLogResponse mapToResponse(CallLog callLog) {
        CallLogResponse response = new CallLogResponse();
        response.setCallId(callLog.getCallId());
        response.setPatientId(callLog.getPatientId());
        response.setScheduleId(callLog.getScheduleId());
        response.setCallStatus(callLog.getCallStatus());
        response.setIvrResponse(callLog.getIvrResponse());
        response.setCallDurationSeconds(callLog.getCallDurationSeconds());
        response.setCallInitiatedAt(callLog.getCallInitiatedAt());
        response.setCallAnsweredAt(callLog.getCallAnsweredAt());
        response.setCallEndedAt(callLog.getCallEndedAt());
        return response;
    }
}
