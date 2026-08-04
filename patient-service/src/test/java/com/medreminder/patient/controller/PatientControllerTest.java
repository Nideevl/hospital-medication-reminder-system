package com.medreminder.patient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medreminder.patient.dto.CreatePatientRequest;
import com.medreminder.patient.dto.UpdatePatientRequest;
import com.medreminder.patient.entity.Patient;
import com.medreminder.patient.exception.ResourceNotFoundException;
import com.medreminder.patient.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
@WithMockUser
@DisplayName("Patient Controller Unit Tests")
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientService patientService;

    private Patient testPatient;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testPatient = Patient.builder()
                .patientId(testId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .medicalRecordNumber("MRN12345")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/patients should return 201 CREATED with patient")
    void testCreatePatient_Success() throws Exception {
        CreatePatientRequest request = new CreatePatientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setPhoneNumber("+1234567890");
        request.setMedicalRecordNumber("MRN12345");

        when(patientService.createPatient(
                eq("John"), eq("Doe"), eq("john.doe@example.com"), eq("+1234567890"), eq("MRN12345")
        )).thenReturn(testPatient);

        mockMvc.perform(post("/api/patients")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(testId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(patientService, times(1)).createPatient(
                eq("John"), eq("Doe"), eq("john.doe@example.com"), eq("+1234567890"), eq("MRN12345")
        );
    }

    @Test
    @DisplayName("GET /api/patients/{id} should return 200 OK with patient")
    void testGetPatient_Success() throws Exception {
        when(patientService.getPatientById(testId)).thenReturn(testPatient);

        mockMvc.perform(get("/api/patients/{id}", testId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(testId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(patientService, times(1)).getPatientById(testId);
    }

    @Test
    @DisplayName("GET /api/patients/{id} when not found should return 404 NOT FOUND")
    void testGetPatient_NotFound() throws Exception {
        when(patientService.getPatientById(testId))
                .thenThrow(new ResourceNotFoundException("Patient not found with ID: " + testId));

        mockMvc.perform(get("/api/patients/{id}", testId)
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(patientService, times(1)).getPatientById(testId);
    }

    @Test
    @DisplayName("GET /api/patients/email/{email} should return 200 OK with patient")
    void testGetPatientByEmail_Success() throws Exception {
        String email = "john.doe@example.com";
        when(patientService.getPatientByEmail(email)).thenReturn(testPatient);

        mockMvc.perform(get("/api/patients/email/{email}", email)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(testId.toString()))
                .andExpect(jsonPath("$.email").value(email));

        verify(patientService, times(1)).getPatientByEmail(email);
    }

    @Test
    @DisplayName("GET /api/patients/email/{email} when not found should return 404 NOT FOUND")
    void testGetPatientByEmail_NotFound() throws Exception {
        String email = "unknown@example.com";
        when(patientService.getPatientByEmail(email))
                .thenThrow(new ResourceNotFoundException("Patient not found"));

        mockMvc.perform(get("/api/patients/email/{email}", email)
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(patientService, times(1)).getPatientByEmail(email);
    }

    @Test
    @DisplayName("PUT /api/patients/{id} should return 200 OK with updated patient")
    void testUpdatePatient_Success() throws Exception {
        UpdatePatientRequest request = new UpdatePatientRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("jane.doe@example.com");
        request.setPhoneNumber("+1234567890");

        Patient updatedPatient = Patient.builder()
                .patientId(testId)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .phone("+1234567890")
                .active(true)
                .build();

        when(patientService.updatePatient(eq(testId), eq("Jane"), eq("Doe"), eq("jane.doe@example.com"), eq("+1234567890")))
                .thenReturn(updatedPatient);

        mockMvc.perform(put("/api/patients/{id}", testId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"));

        verify(patientService, times(1)).updatePatient(eq(testId), eq("Jane"), eq("Doe"), eq("jane.doe@example.com"), eq("+1234567890"));
    }

    @Test
    @DisplayName("PUT /api/patients/{id} when not found should return 404 NOT FOUND")
    void testUpdatePatient_NotFound() throws Exception {
        UpdatePatientRequest request = new UpdatePatientRequest();
        request.setFirstName("Jane");

        when(patientService.updatePatient(eq(testId), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Patient not found"));

        mockMvc.perform(put("/api/patients/{id}", testId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/patients/{id} should return 204 NO CONTENT")
    void testDeletePatient_Success() throws Exception {
        doNothing().when(patientService).deletePatient(testId);

        mockMvc.perform(delete("/api/patients/{id}", testId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(patientService, times(1)).deletePatient(testId);
    }

    @Test
    @DisplayName("GET /api/patients should return 200 OK with active patients")
    void testGetAllPatients_Success() throws Exception {
        List<Patient> patients = Arrays.asList(
                testPatient,
                Patient.builder().patientId(UUID.randomUUID()).firstName("Alice").lastName("Johnson").build()
        );
        when(patientService.getAllActivePatients()).thenReturn(patients);

        mockMvc.perform(get("/api/patients")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].firstName").value("Alice"));

        verify(patientService, times(1)).getAllActivePatients();
    }
}