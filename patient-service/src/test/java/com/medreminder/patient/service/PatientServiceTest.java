package com.medreminder.patient.service;

import com.medreminder.patient.entity.Patient;
import com.medreminder.patient.exception.ResourceNotFoundException;
import com.medreminder.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Service Unit Tests")
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
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
    @DisplayName("Should create patient successfully")
    void testCreatePatient_Success() {
        // Arrange
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);

        // Act
        Patient result = patientService.createPatient(testPatient);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPatientId()).isEqualTo(testId);
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(patientRepository, times(1)).save(testPatient);
    }

    @Test
    @DisplayName("Should throw exception when creating patient with null input")
    void testCreatePatient_NullInput() {
        // Arrange
        when(patientRepository.save(any(Patient.class)))
                .thenThrow(new IllegalArgumentException("Patient cannot be null"));

        // Act & Assert
        assertThatThrownBy(() -> patientService.createPatient(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Patient cannot be null");
    }

    @Test
    @DisplayName("Should retrieve patient by ID successfully")
    void testGetPatientById_Success() {
        // Arrange
        when(patientRepository.findById(testId)).thenReturn(Optional.of(testPatient));

        // Act
        Patient result = patientService.getPatientById(testId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPatientId()).isEqualTo(testId);
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(patientRepository, times(1)).findById(testId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when patient not found by ID")
    void testGetPatientById_NotFound() {
        // Arrange
        when(patientRepository.findById(testId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> patientService.getPatientById(testId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found with ID: " + testId);
        verify(patientRepository, times(1)).findById(testId);
    }

    @Test
    @DisplayName("Should retrieve patient by phone successfully")
    void testGetPatientByPhone_Success() {
        // Arrange
        String phone = "+1234567890";
        when(patientRepository.findByPhone(phone)).thenReturn(Optional.of(testPatient));

        // Act
        Optional<Patient> result = patientService.getPatientByPhone(phone);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getPhone()).isEqualTo(phone);
        verify(patientRepository, times(1)).findByPhone(phone);
    }

    @Test
    @DisplayName("Should return empty when patient not found by phone")
    void testGetPatientByPhone_NotFound() {
        // Arrange
        String phone = "+9999999999";
        when(patientRepository.findByPhone(phone)).thenReturn(Optional.empty());

        // Act
        Optional<Patient> result = patientService.getPatientByPhone(phone);

        // Assert
        assertThat(result).isEmpty();
        verify(patientRepository, times(1)).findByPhone(phone);
    }

    @Test
    @DisplayName("Should update patient successfully")
    void testUpdatePatient_Success() {
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

        when(patientRepository.findById(testId)).thenReturn(Optional.of(testPatient));
        when(patientRepository.save(any(Patient.class))).thenReturn(updatedPatient);

        // Act
        Patient result = patientService.updatePatient(testId, updatedPatient);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Jane Doe");
        assertThat(result.getAddress()).isEqualTo("456 Oak St");
        verify(patientRepository, times(1)).findById(testId);
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should partially update patient - only specified fields")
    void testUpdatePatient_PartialUpdate() {
        // Arrange
        Patient partialUpdate = Patient.builder()
                .name("Jane Smith")
                .build();

        when(patientRepository.findById(testId)).thenReturn(Optional.of(testPatient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient saved = invocation.getArgument(0);
            return saved;
        });

        // Act
        Patient result = patientService.updatePatient(testId, partialUpdate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Jane Smith");
        assertThat(result.getPhone()).isEqualTo("+1234567890"); // Unchanged
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com"); // Unchanged
        verify(patientRepository, times(1)).findById(testId);
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should delete patient successfully")
    void testDeletePatient_Success() {
        // Arrange
        when(patientRepository.findById(testId)).thenReturn(Optional.of(testPatient));
        doNothing().when(patientRepository).delete(testPatient);

        // Act
        patientService.deletePatient(testId);

        // Assert
        verify(patientRepository, times(1)).findById(testId);
        verify(patientRepository, times(1)).delete(testPatient);
    }

    @Test
    @DisplayName("Should retrieve all patients successfully")
    void testGetAllPatients_Success() {
        // Arrange
        List<Patient> patients = Arrays.asList(testPatient, 
                Patient.builder()
                        .patientId(UUID.randomUUID())
                        .name("Alice Johnson")
                        .phone("+1987654321")
                        .email("alice@example.com")
                        .build()
        );
        when(patientRepository.findAll()).thenReturn(patients);

        // Act
        List<Patient> result = patientService.getAllPatients();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Patient::getName)
                .contains("John Doe", "Alice Johnson");
        verify(patientRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no patients exist")
    void testGetAllPatients_Empty() {
        // Arrange
        when(patientRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<Patient> result = patientService.getAllPatients();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(patientRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should verify repository method called with correct parameters using ArgumentCaptor")
    void testCreatePatient_ArgumentCaptor() {
        // Arrange
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        when(patientRepository.save(patientCaptor.capture())).thenReturn(testPatient);

        // Act
        patientService.createPatient(testPatient);

        // Assert
        verify(patientRepository).save(patientCaptor.capture());
        Patient captured = patientCaptor.getValue();
        assertThat(captured).isNotNull();
        assertThat(captured.getName()).isEqualTo("John Doe");
        assertThat(captured.getPhone()).isEqualTo("+1234567890");
    }
}
