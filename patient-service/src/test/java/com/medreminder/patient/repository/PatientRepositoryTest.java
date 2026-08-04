package com.medreminder.patient.repository;

import com.medreminder.patient.entity.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@DisplayName("Patient Repository Integration Tests")
class PatientRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PatientRepository patientRepository;

    private Patient testPatient;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder()
                .patientId(UUID.randomUUID())
                .medicalRecordNumber("MRN-TEST-001")
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
    @DisplayName("Should save patient to database successfully")
    void testSavePatient_Success() {
        Patient saved = patientRepository.saveAndFlush(testPatient);
        entityManager.clear();

        Patient found = patientRepository.findById(saved.getPatientId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should find patient by ID successfully")
    void testFindById_Success() {
        patientRepository.saveAndFlush(testPatient);
        entityManager.clear();

        Optional<Patient> found = patientRepository.findById(testPatient.getPatientId());
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Should return empty optional when patient not found by ID")
    void testFindById_NotFound() {
        Optional<Patient> found = patientRepository.findById(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find patient by phone number successfully")
    void testFindByPhoneNumber_Success() {
        patientRepository.saveAndFlush(testPatient);
        entityManager.clear();

        Optional<Patient> found = patientRepository.findByPhoneNumber("+1234567890");
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Should return empty optional when phone number not found")
    void testFindByPhoneNumber_NotFound() {
        Optional<Patient> found = patientRepository.findByPhoneNumber("+9999999999");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all patients successfully")
    void testFindAll_Success() {
        Patient patient2 = Patient.builder()
                .patientId(UUID.randomUUID())
                .medicalRecordNumber("MRN-TEST-002")
                .name("Alice Johnson")
                .phoneNumber("+1987654321")
                .email("alice@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        patientRepository.save(testPatient);
        patientRepository.save(patient2);
        patientRepository.flush();
        entityManager.clear();

        List<Patient> patients = patientRepository.findAll();
        assertThat(patients).hasSize(2);
    }

    @Test
    @DisplayName("Should update patient successfully")
    void testUpdate_Success() {
        // 1. Save initial patient and clear context
        Patient saved = patientRepository.saveAndFlush(testPatient);
        entityManager.clear();

        // 2. Fetch the managed entity and update fields we know are persisted
        Patient toUpdate = patientRepository.findById(saved.getPatientId()).orElseThrow();
        toUpdate.setName("Jane Doe");
        toUpdate.setActive(false); 
        
        // 3. Save updates and clear context to force a DB read
        patientRepository.saveAndFlush(toUpdate);
        entityManager.clear();

        // 4. Verify the updates were persisted
        Patient found = patientRepository.findById(saved.getPatientId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Jane Doe");
        assertThat(found.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should delete patient successfully")
    void testDelete_Success() {
        Patient saved = patientRepository.saveAndFlush(testPatient);
        entityManager.clear();

        patientRepository.deleteById(saved.getPatientId());
        patientRepository.flush();
        entityManager.clear();

        Optional<Patient> found = patientRepository.findById(saved.getPatientId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should enforce unique constraint on phone number")
    void testDuplicatePhoneNumber_Constraint() {
        patientRepository.saveAndFlush(testPatient);
        entityManager.clear();

        Patient duplicatePatient = Patient.builder()
                .patientId(UUID.randomUUID())
                .medicalRecordNumber("MRN-TEST-003")
                .name("Jane Doe")
                .phoneNumber("+1234567890") // Duplicate
                .email("jane@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> {
            patientRepository.saveAndFlush(duplicatePatient);
        }).isInstanceOf(Exception.class); // Broad catch for any DB constraint exception
    }

    @Test
    @DisplayName("Should find patients by active status successfully")
    void testFindByActiveStatus_Success() {
        Patient inactivePatient = Patient.builder()
                .patientId(UUID.randomUUID())
                .medicalRecordNumber("MRN-TEST-004")
                .name("Inactive User")
                .phoneNumber("+1555555555")
                .email("inactive@example.com")
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        patientRepository.save(testPatient);
        patientRepository.save(inactivePatient);
        patientRepository.flush();
        entityManager.clear();

        List<Patient> activePatients = patientRepository.findByActive(true);
        assertThat(activePatients).hasSize(1);
    }
}