package com.medreminder.patient.service;

import com.medreminder.patient.entity.Patient;
import com.medreminder.patient.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    public Patient getPatientById(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + patientId));
    }

    public Patient getPatientByPhoneNumber(String phoneNumber) {
        return patientRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found with phone: " + phoneNumber));
    }

    public Patient createPatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public Patient updatePatient(UUID patientId, Patient patient) {
        Patient existingPatient = getPatientById(patientId);
        existingPatient.setFirstName(patient.getFirstName());
        existingPatient.setLastName(patient.getLastName());
        existingPatient.setPhoneNumber(patient.getPhoneNumber());
        existingPatient.setLocation(patient.getLocation());
        existingPatient.setHospitalId(patient.getHospitalId());
        return patientRepository.save(existingPatient);
    }

    public void deletePatient(UUID patientId) {
        Patient patient = getPatientById(patientId);
        patientRepository.delete(patient);
    }
}