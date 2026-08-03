package com.medreminder.patient.dto;

import com.medreminder.patient.entity.Patient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {
    private UUID patientId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String medicalRecordNumber;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PatientResponse(Patient patient) {
        this.patientId = patient.getPatientId();
        this.firstName = patient.getFirstName();
        this.lastName = patient.getLastName();
        this.email = patient.getEmail();
        this.phoneNumber = patient.getPhoneNumber();
        this.medicalRecordNumber = patient.getMedicalRecordNumber();
        this.active = patient.getActive();
        this.createdAt = patient.getCreatedAt();
        this.updatedAt = patient.getUpdatedAt();
    }
}
