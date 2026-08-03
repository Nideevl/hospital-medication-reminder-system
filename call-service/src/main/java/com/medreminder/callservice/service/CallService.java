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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CallService {

    @Autowired
    private CallLogRepository callLogRepository;

    @Autowired
    private CallAttemptRepository callAttemptRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public CallLogResponse initiateCall(InitiateCallRequest request) {
        log.info("Initiating call for patient: {}, schedule: {}", request.getPatientId(), request.getScheduleId());

        try {
            CallLog callLog = new CallLog();
            callLog.setPatientId(request.getPatientId());
            callLog.setScheduleId(request.getScheduleId());
            callLog.setCallStatus("INITIATED");
            callLog.setAttemptNotes("Medication: " + request.getMedicationName() + " - " + request.getDosage());

            CallLog savedCallLog = callLogRepository.save(callLog);

            CallAttempt firstAttempt = new CallAttempt();
            firstAttempt.setCallId(savedCallLog.getCallId());
            firstAttempt.setAttemptNumber(1);
            firstAttempt.setStatus("INITIATED");
            callAttemptRepository.save(firstAttempt);

            log.info("Call initiated successfully with ID: {}", savedCallLog.getCallId());

            return mapToResponse(savedCallLog);

        } catch (Exception e) {
            log.error("Error initiating call for patient: {}", request.getPatientId(), e);
            throw new CallInitiationException("Failed to initiate call: " + e.getMessage(), e);
        }
    }

    @Transactional
    public CallLogResponse updateCallResponse(UpdateCallResponseRequest request) {
        log.info("Updating call response for call: {}", request.getCallId());

        CallLog callLog = callLogRepository.findByCallId(request.getCallId())
                .orElseThrow(() -> new CallNotFoundException("Call not found: " + request.getCallId()));

        callLog.setCallStatus("ANSWERED");
        callLog.setIvrResponse(request.getIvrResponse());
        callLog.setCallDurationSeconds(request.getCallDurationSeconds());
        callLog.setCallAnsweredAt(LocalDateTime.now());

        CallLog updatedCallLog = callLogRepository.save(callLog);

        try {
            CallResponseEvent event = new CallResponseEvent();
            event.setCallId(updatedCallLog.getCallId());
            event.setPatientId(updatedCallLog.getPatientId());
            event.setScheduleId(updatedCallLog.getScheduleId());
            event.setResponse(request.getIvrResponse());
            event.setTimestamp(System.currentTimeMillis());

            kafkaTemplate.send("call-response-received", event);
            log.info("Published call response event to Kafka");
        } catch (Exception e) {
            log.warn("Failed to publish call response event: {}", e.getMessage());
        }

        return mapToResponse(updatedCallLog);
    }

    public CallLogResponse getCallLog(UUID callId) {
        CallLog callLog = callLogRepository.findByCallId(callId)
                .orElseThrow(() -> new CallNotFoundException("Call not found: " + callId));

        return mapToResponse(callLog);
    }

    public List<CallLogResponse> getPatientCallHistory(UUID patientId) {
        List<CallLog> callLogs = callLogRepository.findByPatientId(patientId);
        return callLogs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<CallAttempt> getCallAttempts(UUID callId) {
        return callAttemptRepository.findByCallIdOrderByAttemptNumber(callId);
    }

    private CallLogResponse mapToResponse(CallLog callLog) {
        return new CallLogResponse(
                callLog.getCallId(),
                callLog.getPatientId(),
                callLog.getScheduleId(),
                callLog.getCallStatus(),
                callLog.getIvrResponse(),
                callLog.getCallDurationSeconds(),
                callLog.getCallInitiatedAt(),
                callLog.getCallAnsweredAt()
        );
    }
}
