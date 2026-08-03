package com.medreminder.patient.controller;

import com.medreminder.patient.dto.CreatePatientRequest;
import com.medreminder.patient.dto.PatientResponse;
import com.medreminder.patient.dto.UpdatePatientRequest;
import com.medreminder.patient.entity.Patient;
import com.medreminder.patient.service.PatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
@Slf4j
public class PatientController {

    @Autowired
    private PatientService patientService;

    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(@RequestBody CreatePatientRequest request) {
        log.info("Creating patient: {}", request.getEmail());
        Patient patient = patientService.createPatient(
            request.getFirstName(),
            request.getLastName(),
            request.getEmail(),
            request.getPhoneNumber(),
            request.getMedicalRecordNumber()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new PatientResponse(patient));
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<PatientResponse> getPatient(@PathVariable UUID patientId) {
        log.info("Fetching patient: {}", patientId);
        Patient patient = patientService.getPatientById(patientId);
        return ResponseEntity.ok(new PatientResponse(patient));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<PatientResponse> getPatientByEmail(@PathVariable String email) {
        log.info("Fetching patient by email: {}", email);
        Patient patient = patientService.getPatientByEmail(email);
        return ResponseEntity.ok(new PatientResponse(patient));
    }

    @GetMapping("/mrn/{mrn}")
    public ResponseEntity<PatientResponse> getPatientByMrn(@PathVariable String mrn) {
        log.info("Fetching patient by MRN: {}", mrn);
        Patient patient = patientService.getPatientByMedicalRecordNumber(mrn);
        return ResponseEntity.ok(new PatientResponse(patient));
    }

    @GetMapping
    public ResponseEntity<List<PatientResponse>> getAllActivePatients() {
        log.info("Fetching all active patients");
        List<Patient> patients = patientService.getAllActivePatients();
        List<PatientResponse> responses = patients.stream()
                .map(PatientResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable UUID patientId,
            @RequestBody UpdatePatientRequest request) {
        log.info("Updating patient: {}", patientId);
        Patient patient = patientService.updatePatient(
            patientId,
            request.getFirstName(),
            request.getLastName(),
            request.getEmail(),
            request.getPhoneNumber()
        );
        return ResponseEntity.ok(new PatientResponse(patient));
    }

    @PutMapping("/{patientId}/deactivate")
    public ResponseEntity<Void> deactivatePatient(@PathVariable UUID patientId) {
        log.info("Deactivating patient: {}", patientId);
        patientService.deactivatePatient(patientId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{patientId}/activate")
    public ResponseEntity<Void> activatePatient(@PathVariable UUID patientId) {
        log.info("Activating patient: {}", patientId);
        patientService.activatePatient(patientId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID patientId) {
        log.info("Deleting patient: {}", patientId);
        patientService.deletePatient(patientId);
        return ResponseEntity.noContent().build();
    }
}
