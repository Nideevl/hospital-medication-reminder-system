package com.medreminder.scheduleservice.service;

import com.medreminder.scheduleservice.entity.Medication;
import com.medreminder.scheduleservice.exception.ResourceNotFoundException;
import com.medreminder.scheduleservice.repository.MedicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MedicationService {
    @Autowired
    private MedicationRepository medicationRepository;

    public Medication createMedication(Medication medication) {
        return medicationRepository.save(medication);
    }

    public Medication getMedicationById(UUID medicationId) {
        return medicationRepository.findById(medicationId).orElseThrow(() -> new ResourceNotFoundException("Medication not found"));
    }

    public Medication updateMedication(Medication medication) {
        getMedicationById(medication.getMedicationId());
        return medicationRepository.save(medication);
    }

    public void deleteMedication(UUID medicationId) {
        Medication medication = getMedicationById(medicationId);
        medicationRepository.delete(medication);
    }
}