package com.medreminder.escalationservice.controller;

import com.medreminder.escalationservice.dto.EscalationRequest;
import com.medreminder.escalationservice.dto.EscalationResponse;
import com.medreminder.escalationservice.service.EscalationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/escalations")
@Slf4j
public class EscalationController {

    @Autowired
    private EscalationService escalationService;

    @PostMapping("/trigger")
    public ResponseEntity<EscalationResponse> triggerEscalation(@RequestBody EscalationRequest request) {
        log.info("POST /api/escalations/trigger - Patient: {}", request.getPatientId());
        EscalationResponse response = escalationService.triggerEscalation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{escalationId}")
    public ResponseEntity<EscalationResponse> getEscalation(@PathVariable UUID escalationId) {
        log.info("GET /api/escalations/{}", escalationId);
        return ResponseEntity.ok(escalationService.getEscalation(escalationId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<EscalationResponse>> getPatientEscalations(@PathVariable UUID patientId) {
        log.info("GET /api/escalations/patient/{}", patientId);
        return ResponseEntity.ok(escalationService.getPatientEscalations(patientId));
    }

    @PutMapping("/{escalationId}/resolve")
    public ResponseEntity<EscalationResponse> resolveEscalation(@PathVariable UUID escalationId) {
        log.info("PUT /api/escalations/{}/resolve", escalationId);
        return ResponseEntity.ok(escalationService.resolveEscalation(escalationId));
    }
}
