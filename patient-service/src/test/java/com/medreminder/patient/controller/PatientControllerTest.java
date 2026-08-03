package com.medreminder.patient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
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
                .name("John Doe")
                .phone("+1234567890")
                .email("john.doe@example.com")
                .address("123 Main St")
                .dateOfBirth("1980-01-01")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /patients should return 201 CREATED with patient")
    void testCreatePatient_Success() throws Exception {
        // Arrange
        when(patientService.createPatient(any(Patient.class))).thenReturn(testPatient);

        // Act & Assert
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPatient)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(testId.toString()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.phone").value("+1234567890"))
                .andExpect(header().exists("Location"));

        verify(patientService, times(1)).createPatient(any(Patient.class));
    }

    @Test
    @DisplayName("POST /patients with invalid data should return 400 BAD REQUEST")
    void testCreatePatient_BadRequest() throws Exception {
        // Arrange
        Patient invalidPatient = new Patient(); // Empty patient

        // Act & Assert
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPatient)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /patients/{id} should return 200 OK with patient")
    void testGetPatient_Success() throws Exception {
        // Arrange
        when(patientService.getPatientById(testId)).thenReturn(testPatient);

        // Act & Assert
        mockMvc.perform(get("/patients/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(testId.toString()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.phone").value("+1234567890"));

        verify(patientService, times(1)).getPatientById(testId);
    }

    @Test
    @DisplayName("GET /patients/{id} when not found should return 404 NOT FOUND")
    void testGetPatient_NotFound() throws Exception {
        // Arrange
        when(patientService.getPatientById(testId))
                .thenThrow(new ResourceNotFoundException("Patient not found with ID: " + testId));

        // Act & Assert
        mockMvc.perform(get("/patients/{id}", testId))
                .andExpect(status().isNotFound());

        verify(patientService, times(1)).getPatientById(testId);
    }

    @Test
    @DisplayName("GET /patients/phone/{phone} should return 200 OK with patient")
    void testGetPatientByPhone_Success() throws Exception {
        // Arrange
        String phone = "+1234567890";
        when(patientService.getPatientByPhone(phone)).thenReturn(java.util.Optional.of(testPatient));

        // Act & Assert
        mockMvc.perform(get("/patients/phone/{phone}", phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(testId.toString()))
                .andExpect(jsonPath("$.phone").value(phone));

        verify(patientService, times(1)).getPatientByPhone(phone);
    }

    @Test
    @DisplayName("GET /patients/phone/{phone} when not found should return 404 NOT FOUND")
    void testGetPatientByPhone_NotFound() throws Exception {
        // Arrange
        String phone = "+9999999999";
        when(patientService.getPatientByPhone(phone)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/patients/phone/{phone}", phone))
                .andExpect(status().isNotFound());

        verify(patientService, times(1)).getPatientByPhone(phone);
    }

    @Test
    @DisplayName("PUT /patients/{id} should return 200 OK with updated patient")
    void testUpdatePatient_Success() throws Exception {
        // Arrange
        Patient updatedPatient = Patient.builder()
                .patientId(testId)
                .name("Jane Doe")
                .phone("+1234567890")
                .email("jane.doe@example.com")
                .address("456 Oak St")
                .dateOfBirth("1980-01-01")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(patientService.updatePatient(eq(testId), any(Patient.class))).thenReturn(updatedPatient);

        // Act & Assert
        mockMvc.perform(put("/patients/{id}", testId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedPatient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.address").value("456 Oak St"));

        verify(patientService, times(1)).updatePatient(eq(testId), any(Patient.class));
    }

    @Test
    @DisplayName("PUT /patients/{id} when not found should return 404 NOT FOUND")
    void testUpdatePatient_NotFound() throws Exception {
        // Arrange
        when(patientService.updatePatient(eq(testId), any(Patient.class)))
                .thenThrow(new ResourceNotFoundException("Patient not found with ID: " + testId));

        // Act & Assert
        mockMvc.perform(put("/patients/{id}", testId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPatient)))
                .andExpect(status().isNotFound());

        verify(patientService, times(1)).updatePatient(eq(testId), any(Patient.class));
    }

    @Test
    @DisplayName("DELETE /patients/{id} should return 204 NO CONTENT")
    void testDeletePatient_Success() throws Exception {
        // Arrange
        doNothing().when(patientService).deletePatient(testId);

        // Act & Assert
        mockMvc.perform(delete("/patients/{id}", testId))
                .andExpect(status().isNoContent());

        verify(patientService, times(1)).deletePatient(testId);
    }

    @Test
    @DisplayName("GET /patients should return 200 OK with list of patients")
    void testGetAllPatients_Success() throws Exception {
        // Arrange
        List<Patient> patients = Arrays.asList(testPatient,
                Patient.builder()
                        .patientId(UUID.randomUUID())
                        .name("Alice Johnson")
                        .phone("+1987654321")
                        .email("alice@example.com")
                        .build()
        );
        when(patientService.getAllPatients()).thenReturn(patients);

        // Act & Assert
        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[1].name").value("Alice Johnson"));

        verify(patientService, times(1)).getAllPatients();
    }
}
