package com.medreminder.scheduleservice.controller;

import com.medreminder.scheduleservice.entity.Medication;
import com.medreminder.scheduleservice.service.MedicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {
    @Autowired
    private MedicationService medicationService;

    @PostMapping
    public ResponseEntity<Medication> createMedication(@RequestBody Medication medication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicationService.createMedication(medication));
    }

    @GetMapping("/{medicationId}")
    public ResponseEntity<Medication> getMedication(@PathVariable UUID medicationId) {
        return ResponseEntity.ok(medicationService.getMedicationById(medicationId));
    }

    @PutMapping("/{medicationId}")
    public ResponseEntity<Medication> updateMedication(@PathVariable UUID medicationId, @RequestBody Medication medication) {
        medication.setMedicationId(medicationId);
        return ResponseEntity.ok(medicationService.updateMedication(medication));
    }

    @DeleteMapping("/{medicationId}")
    public ResponseEntity<Void> deleteMedication(@PathVariable UUID medicationId) {
        medicationService.deleteMedication(medicationId);
        return ResponseEntity.noContent().build();
    }
}