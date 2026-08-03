package com.medreminder.patient.service;

import com.medreminder.patient.entity.Patient;
import com.medreminder.patient.exception.ResourceNotFoundException;
import com.medreminder.patient.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    public Patient createPatient(String firstName, String lastName, String email, String phoneNumber, String medicalRecordNumber) {
        Patient patient = new Patient();
        patient.setPatientId(UUID.randomUUID());
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setEmail(email);
        patient.setPhoneNumber(phoneNumber);
        patient.setMedicalRecordNumber(medicalRecordNumber);
        patient.setActive(true);
        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());
        
        log.info("Creating patient: {} {}", firstName, lastName);
        return patientRepository.save(patient);
    }

    public Patient getPatientById(UUID patientId) {
        log.info("Fetching patient with ID: {}", patientId);
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));
    }

    public Patient getPatientByEmail(String email) {
        log.info("Fetching patient with email: {}", email);
        return patientRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with email: " + email));
    }

    public Patient getPatientByMedicalRecordNumber(String mrn) {
        log.info("Fetching patient with MRN: {}", mrn);
        return patientRepository.findByMedicalRecordNumber(mrn)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with MRN: " + mrn));
    }

    public List<Patient> getAllActivePatients() {
        log.info("Fetching all active patients");
        return patientRepository.findByActiveTrue();
    }

    public Patient updatePatient(UUID patientId, String firstName, String lastName, String email, String phoneNumber) {
        Patient patient = getPatientById(patientId);
        
        if (firstName != null && !firstName.isBlank()) patient.setFirstName(firstName);
        if (lastName != null && !lastName.isBlank()) patient.setLastName(lastName);
        if (email != null && !email.isBlank()) patient.setEmail(email);
        if (phoneNumber != null && !phoneNumber.isBlank()) patient.setPhoneNumber(phoneNumber);
        
        patient.setUpdatedAt(LocalDateTime.now());
        log.info("Updating patient: {}", patientId);
        return patientRepository.save(patient);
    }

    public void deactivatePatient(UUID patientId) {
        Patient patient = getPatientById(patientId);
        patient.setActive(false);
        patient.setUpdatedAt(LocalDateTime.now());
        log.info("Deactivating patient: {}", patientId);
        patientRepository.save(patient);
    }

    public void activatePatient(UUID patientId) {
        Patient patient = getPatientById(patientId);
        patient.setActive(true);
        patient.setUpdatedAt(LocalDateTime.now());
        log.info("Activating patient: {}", patientId);
        patientRepository.save(patient);
    }

    public void deletePatient(UUID patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with ID: " + patientId);
        }
        log.info("Deleting patient: {}", patientId);
        patientRepository.deleteById(patientId);
    }
}
