package com.medreminder.patient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePatientRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String medicalRecordNumber;
}
