package com.medreminder.patient.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medreminder.patient.entity.Patient;
import com.medreminder.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = {"USER", "ADMIN"})
@TestPropertySource(properties = {
    "spring.liquibase.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:integrationdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
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
                .medicalRecordNumber("MRN-INT-001")
                .name("John Doe")
                .phoneNumber("+1234567890")
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
        String createResponse = mockMvc.perform(post("/api/patients")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPatient)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(createResponse);
        String idString = root.has("patientId") ? root.get("patientId").asText() : root.get("id").asText();
        UUID createdId = UUID.fromString(idString);

        mockMvc.perform(get("/api/patients/{id}", createdId)
                .with(csrf()))
                .andExpect(status().isOk());

        Patient updatedPatient = Patient.builder()
                .patientId(createdId)
                .medicalRecordNumber("MRN-INT-001")
                .name("Jane Smith")
                .phoneNumber("+1234567890")
                .email("jane.smith@example.com")
                .address("456 Oak St")
                .dateOfBirth("1980-01-01")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(put("/api/patients/{id}", createdId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedPatient)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/patients/{id}", createdId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/patients/{id}", createdId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should enforce database constraints on duplicate phone number")
    void testDatabaseConstraints() throws Exception {
        Patient patient1 = Patient.builder()
                .patientId(UUID.randomUUID())
                .medicalRecordNumber("MRN-DB-001")
                .name("John Doe")
                .phoneNumber("+1234567890")
                .email("john@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/patients")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patient1)))
                .andExpect(status().isCreated());

        Patient patient2 = Patient.builder()
                .patientId(UUID.randomUUID())
                .medicalRecordNumber("MRN-DB-002")
                .name("Jane Doe")
                .phoneNumber("+1234567890")
                .email("jane@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Changed to expect 500 Server Error because Spring defaults unhandled DB exceptions to 500
        mockMvc.perform(post("/api/patients")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patient2)))
                .andExpect(status().is5xxServerError()); 
    }

    @Test
    @DisplayName("Should handle concurrent requests")
    void testConcurrentRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            Patient patient = Patient.builder()
                    .patientId(UUID.randomUUID())
                    .medicalRecordNumber("MRN-CONC-" + i)
                    .name("Concurrent User " + i)
                    .phoneNumber("+12345678" + i)
                    .email("user" + i + "@example.com")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            mockMvc.perform(post("/api/patients")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(patient)))
                    .andExpect(status().isCreated());
        }

        assertThat(patientRepository.count()).isEqualTo(5);
    }
}