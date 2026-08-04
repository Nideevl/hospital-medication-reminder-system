package com.medreminder.patient.service;

import com.medreminder.patient.entity.Patient;
import com.medreminder.patient.exception.ResourceNotFoundException;
import com.medreminder.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    // --- OVERLOADS & METHODS REQUIRED BY TESTS --- //

    public Patient createPatient(Patient patient) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient cannot be null");
        }
        if (patient.getPatientId() == null) {
            patient.setPatientId(UUID.randomUUID());
        }
        if (patient.getCreatedAt() == null) {
            patient.setCreatedAt(LocalDateTime.now());
        }
        patient.setUpdatedAt(LocalDateTime.now());
        log.info("Creating patient from object: {}", patient.getFirstName());
        return patientRepository.save(patient);
    }

    public Patient updatePatient(UUID patientId, Patient patientDetails) {
        Patient patient = getPatientById(patientId);

        if (patientDetails.getName() != null) patient.setName(patientDetails.getName());
        if (patientDetails.getFirstName() != null) patient.setFirstName(patientDetails.getFirstName());
        if (patientDetails.getLastName() != null) patient.setLastName(patientDetails.getLastName());
        if (patientDetails.getEmail() != null) patient.setEmail(patientDetails.getEmail());
        if (patientDetails.getPhoneNumber() != null) patient.setPhoneNumber(patientDetails.getPhoneNumber());
        if (patientDetails.getPhone() != null) patient.setPhone(patientDetails.getPhone());
        if (patientDetails.getAddress() != null) patient.setAddress(patientDetails.getAddress());
        if (patientDetails.getActive() != null) patient.setActive(patientDetails.getActive());

        patient.setUpdatedAt(LocalDateTime.now());
        log.info("Updating patient from object: {}", patientId);
        return patientRepository.save(patient);
    }

    public Optional<Patient> getPatientByPhone(String phone) {
        log.info("Fetching patient with phone: {}", phone);
        return patientRepository.findByPhone(phone);
    }

    public List<Patient> getAllPatients() {
        log.info("Fetching all patients");
        return patientRepository.findAll();
    }

    public void deletePatient(UUID patientId) {
        log.info("Deleting patient: {}", patientId);
        Patient patient = getPatientById(patientId);
        patientRepository.delete(patient);
    }

    // --- ORIGINAL DOMAIN METHODS --- //

    public Patient createPatient(String firstName, String lastName, String email, String phoneNumber, String medicalRecordNumber) {
        Patient patient = Patient.builder()
                .patientId(UUID.randomUUID())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .medicalRecordNumber(medicalRecordNumber)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

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
}