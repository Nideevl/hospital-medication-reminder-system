package com.medreminder.callservice.controller;

import com.medreminder.callservice.dto.CallLogResponse;
import com.medreminder.callservice.dto.InitiateCallRequest;
import com.medreminder.callservice.dto.UpdateCallResponseRequest;
import com.medreminder.callservice.entity.CallAttempt;
import com.medreminder.callservice.service.CallService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/calls")
@Slf4j
public class CallController {

    @Autowired
    private CallService callService;

    @PostMapping("/initiate")
    public ResponseEntity<CallLogResponse> initiateCall(@RequestBody InitiateCallRequest request) {
        log.info("POST /api/calls/initiate - Patient: {}", request.getPatientId());
        CallLogResponse response = callService.initiateCall(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/response")
    public ResponseEntity<CallLogResponse> updateCallResponse(@RequestBody UpdateCallResponseRequest request) {
        log.info("POST /api/calls/response - Call: {}", request.getCallId());
        CallLogResponse response = callService.updateCallResponse(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{callId}")
    public ResponseEntity<CallLogResponse> getCall(@PathVariable UUID callId) {
        log.info("GET /api/calls/{}", callId);
        return ResponseEntity.ok(callService.getCallLog(callId));
    }

    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<List<CallLogResponse>> getPatientCallHistory(@PathVariable UUID patientId) {
        log.info("GET /api/calls/patient/{}/history", patientId);
        return ResponseEntity.ok(callService.getPatientCallHistory(patientId));
    }

    @GetMapping("/{callId}/attempts")
    public ResponseEntity<List<CallAttempt>> getCallAttempts(@PathVariable UUID callId) {
        log.info("GET /api/calls/{}/attempts", callId);
        return ResponseEntity.ok(callService.getCallAttempts(callId));
    }
}
