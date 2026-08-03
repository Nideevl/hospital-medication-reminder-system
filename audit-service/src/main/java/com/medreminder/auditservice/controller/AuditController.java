package com.medreminder.auditservice.controller;

import com.medreminder.auditservice.dto.MedicationAuditDTO;
import com.medreminder.auditservice.dto.RecordMedicationRequest;
import com.medreminder.auditservice.dto.UserActionDTO;
import com.medreminder.auditservice.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audits")
@Slf4j
public class AuditController {

    @Autowired
    private AuditService auditService;

    @PostMapping("/medication/record")
    public ResponseEntity<MedicationAuditDTO> recordMedicationEvent(@RequestBody RecordMedicationRequest request) {
        log.info("POST /api/audits/medication/record - Patient: {}", request.getPatientId());
        MedicationAuditDTO response = auditService.recordMedicationEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/medication/{auditId}")
    public ResponseEntity<MedicationAuditDTO> getMedicationAudit(@PathVariable UUID auditId) {
        log.info("GET /api/audits/medication/{}", auditId);
        return ResponseEntity.ok(auditService.getMedicationAudit(auditId));
    }

    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<List<MedicationAuditDTO>> getPatientAuditHistory(@PathVariable UUID patientId) {
        log.info("GET /api/audits/patient/{}/history", patientId);
        return ResponseEntity.ok(auditService.getPatientAuditHistory(patientId));
    }

    @GetMapping("/patient/{patientId}/range")
    public ResponseEntity<List<MedicationAuditDTO>> getAuditsByDateRange(
            @PathVariable UUID patientId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        log.info("GET /api/audits/patient/{}/range - Start: {}, End: {}", patientId, startDate, endDate);
        return ResponseEntity.ok(auditService.getAuditsByDateRange(patientId, startDate, endDate));
    }

    @GetMapping("/patient/{patientId}/missed")
    public ResponseEntity<List<MedicationAuditDTO>> getMissedMedications(@PathVariable UUID patientId) {
        log.info("GET /api/audits/patient/{}/missed", patientId);
        return ResponseEntity.ok(auditService.getMissedMedications(patientId));
    }

    @GetMapping("/user/{userId}/actions")
    public ResponseEntity<List<UserActionDTO>> getUserActions(@PathVariable UUID userId) {
        log.info("GET /api/audits/user/{}/actions", userId);
        return ResponseEntity.ok(auditService.getUserActions(userId));
    }
}
