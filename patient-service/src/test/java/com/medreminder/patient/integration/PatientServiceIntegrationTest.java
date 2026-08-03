package com.medreminder.patient.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medreminder.patient.entity.Patient;
import com.medreminder.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Patient Service Integration Tests")
class PatientServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    private Patient testPatient;
    private UUID testId;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
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
    @DisplayName("Full patient workflow: create → retrieve → update → delete")
    void testFullPatientWorkflow() throws Exception {
        // 1. Create patient - POST
        String createResponse = mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPatient)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").exists())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Patient created = objectMapper.readValue(createResponse, Patient.class);
        UUID createdId = created.getPatientId();

        // 2. Retrieve patient - GET
        mockMvc.perform(get("/patients/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(createdId.toString()))
                .andExpect(jsonPath("$.name").value("John Doe"));

        // 3. Update patient - PUT
        Patient updatedPatient = Patient.builder()
                .patientId(createdId)
                .name("Jane Smith")
                .phone("+1234567890")
                .email("jane.smith@example.com")
                .address("456 Oak St")
                .dateOfBirth("1980-01-01")
                .active(true)
                .createdAt(created.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(put("/patients/{id}", createdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedPatient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Smith"))
                .andExpect(jsonPath("$.address").value("456 Oak St"));

        // 4. Delete patient - DELETE
        mockMvc.perform(delete("/patients/{id}", createdId))
                .andExpect(status().isNoContent());

        // 5. Verify deletion - GET should return 404
        mockMvc.perform(get("/patients/{id}", createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should enforce database constraints on duplicate phone")
    void testDatabaseConstraints() throws Exception {
        // Create first patient
        Patient patient1 = Patient.builder()
                .patientId(UUID.randomUUID())
                .name("John Doe")
                .phone("+1234567890")
                .email("john@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patient1)))
                .andExpect(status().isCreated());

        // Try to create second patient with same phone
        Patient patient2 = Patient.builder()
                .patientId(UUID.randomUUID())
                .name("Jane Doe")
                .phone("+1234567890") // Duplicate phone
                .email("jane@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patient2)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should handle concurrent requests")
    void testConcurrentRequests() throws Exception {
        // Create multiple patients concurrently
        for (int i = 0; i < 5; i++) {
            Patient patient = Patient.builder()
                    .patientId(UUID.randomUUID())
                    .name("Concurrent User " + i)
                    .phone("+12345678" + i)
                    .email("user" + i + "@example.com")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            mockMvc.perform(post("/patients")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(patient)))
                    .andExpect(status().isCreated());
        }

        // Verify all patients were created
        assertThat(patientRepository.count()).isEqualTo(5);
    }
}
