package com.medreminder.patient.repository;

import com.medreminder.patient.entity.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
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
    @DisplayName("Should save patient to database successfully")
    void testSavePatient_Success() {
        // Act
        Patient saved = entityManager.persistAndFlush(testPatient);
        entityManager.clear();

        // Assert
        Patient found = entityManager.find(Patient.class, saved.getPatientId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("John Doe");
        assertThat(found.getPhone()).isEqualTo("+1234567890");
    }

    @Test
    @DisplayName("Should find patient by ID successfully")
    void testFindById_Success() {
        // Arrange
        entityManager.persistAndFlush(testPatient);
        entityManager.clear();

        // Act
        Optional<Patient> found = patientRepository.findById(testPatient.getPatientId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getPhone()).isEqualTo("+1234567890");
    }

    @Test
    @DisplayName("Should return empty optional when patient not found by ID")
    void testFindById_NotFound() {
        // Act
        Optional<Patient> found = patientRepository.findById(UUID.randomUUID());

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find patient by phone successfully")
    void testFindByPhone_Success() {
        // Arrange
        entityManager.persistAndFlush(testPatient);
        entityManager.clear();

        // Act
        Optional<Patient> found = patientRepository.findByPhone("+1234567890");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should return empty optional when phone not found")
    void testFindByPhone_NotFound() {
        // Act
        Optional<Patient> found = patientRepository.findByPhone("+9999999999");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all patients successfully")
    void testFindAll_Success() {
        // Arrange
        Patient patient2 = Patient.builder()
                .patientId(UUID.randomUUID())
                .name("Alice Johnson")
                .phone("+1987654321")
                .email("alice@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        entityManager.persist(testPatient);
        entityManager.persist(patient2);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Patient> patients = patientRepository.findAll();

        // Assert
        assertThat(patients).hasSize(2);
        assertThat(patients).extracting(Patient::getName)
                .contains("John Doe", "Alice Johnson");
    }

    @Test
    @DisplayName("Should update patient successfully")
    void testUpdate_Success() {
        // Arrange
        entityManager.persistAndFlush(testPatient);
        entityManager.clear();

        // Act
        testPatient.setName("Jane Doe");
        testPatient.setAddress("456 Oak St");
        entityManager.merge(testPatient);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Patient found = entityManager.find(Patient.class, testPatient.getPatientId());
        assertThat(found.getName()).isEqualTo("Jane Doe");
        assertThat(found.getAddress()).isEqualTo("456 Oak St");
    }

    @Test
    @DisplayName("Should delete patient successfully")
    void testDelete_Success() {
        // Arrange
        entityManager.persistAndFlush(testPatient);
        entityManager.clear();

        // Act
        entityManager.remove(testPatient);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Patient found = entityManager.find(Patient.class, testPatient.getPatientId());
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("Should enforce unique constraint on phone")
    void testDuplicatePhone_Constraint() {
        // Arrange
        entityManager.persistAndFlush(testPatient);
        entityManager.clear();

        Patient duplicatePatient = Patient.builder()
                .patientId(UUID.randomUUID())
                .name("Jane Doe")
                .phone("+1234567890") // Same phone as testPatient
                .email("jane@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Act & Assert
        assertThatThrownBy(() -> {
            entityManager.persist(duplicatePatient);
            entityManager.flush();
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should find patients by active status successfully")
    void testFindByActiveStatus_Success() {
        // Arrange
        Patient inactivePatient = Patient.builder()
                .patientId(UUID.randomUUID())
                .name("Inactive User")
                .phone("+1555555555")
                .email("inactive@example.com")
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        entityManager.persist(testPatient);
        entityManager.persist(inactivePatient);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Patient> activePatients = patientRepository.findByActive(true);

        // Assert
        assertThat(activePatients).hasSize(1);
        assertThat(activePatients.get(0).getName()).isEqualTo("John Doe");
        assertThat(activePatients.get(0).isActive()).isTrue();
    }
}
