package com.medreminder.auditservice.service;

import com.medreminder.auditservice.entity.MedicationAudit;
import com.medreminder.auditservice.entity.UserAction;
import com.medreminder.auditservice.repository.MedicationAuditRepository;
import com.medreminder.auditservice.repository.UserActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Service Unit Tests")
class AuditServiceTest {

    @Mock
    private MedicationAuditRepository medicationAuditRepository;

    @Mock
    private UserActionRepository userActionRepository;

    @InjectMocks
    private AuditService auditService;

    private MedicationAudit testAudit;
    private UserAction testUserAction;
    private UUID auditId;
    private UUID patientId;
    private UUID medicationId;

    @BeforeEach
    void setUp() {
        auditId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        medicationId = UUID.randomUUID();

        testAudit = MedicationAudit.builder()
                .auditId(auditId)
                .patientId(patientId)
                .medicationId(medicationId)
                .actionType("TAKEN")
                .actionTime(LocalDateTime.now())
                .actionDetails("{\"status\":\"completed\"}")
                .createdAt(LocalDateTime.now())
                .build();

        testUserAction = UserAction.builder()
                .userActionId(UUID.randomUUID())
                .userId("user123")
                .actionType("LOGIN")
                .targetEntity("Patient")
                .targetEntityId(patientId.toString())
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create audit record successfully")
    void testCreateAuditRecord_Success() {
        // Arrange
        when(medicationAuditRepository.save(any(MedicationAudit.class))).thenReturn(testAudit);

        // Act
        MedicationAudit result = auditService.createAuditRecord(
                patientId, medicationId, "TAKEN", "{\"status\":\"completed\"}");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAuditId()).isEqualTo(auditId);
        assertThat(result.getActionType()).isEqualTo("TAKEN");
        verify(medicationAuditRepository, times(1)).save(any(MedicationAudit.class));
    }

    @Test
    @DisplayName("Should retrieve audit trail successfully")
    void testGetAuditTrail_Success() {
        // Arrange
        List<MedicationAudit> audits = Arrays.asList(testAudit);
        when(medicationAuditRepository.findAll()).thenReturn(audits);

        // Act
        List<MedicationAudit> result = auditService.getAuditTrail();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(medicationAuditRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should retrieve audits by patient successfully")
    void testGetAuditsByPatient_Success() {
        // Arrange
        List<MedicationAudit> audits = Arrays.asList(testAudit);
        when(medicationAuditRepository.findByPatientId(patientId)).thenReturn(audits);

        // Act
        List<MedicationAudit> result = auditService.getAuditsByPatient(patientId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(medicationAuditRepository, times(1)).findByPatientId(patientId);
    }

    @Test
    @DisplayName("Should record user action successfully")
    void testRecordUserAction_Success() {
        // Arrange
        when(userActionRepository.save(any(UserAction.class))).thenReturn(testUserAction);

        // Act
        UserAction result = auditService.recordUserAction(
                "user123", "LOGIN", "Patient", patientId.toString(), "192.168.1.1");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getActionType()).isEqualTo("LOGIN");
        verify(userActionRepository, times(1)).save(any(UserAction.class));
    }

    @Test
    @DisplayName("Should retrieve user actions by user ID successfully")
    void testGetUserActionsByUser_Success() {
        // Arrange
        List<UserAction> actions = Arrays.asList(testUserAction);
        when(userActionRepository.findByUserId("user123")).thenReturn(actions);

        // Act
        List<UserAction> result = auditService.getUserActionsByUser("user123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(userActionRepository, times(1)).findByUserId("user123");
    }

    @Test
    @DisplayName("Should retrieve audits by action type successfully")
    void testGetAuditsByActionType_Success() {
        // Arrange
        List<MedicationAudit> audits = Arrays.asList(testAudit);
        when(medicationAuditRepository.findByActionType("TAKEN")).thenReturn(audits);

        // Act
        List<MedicationAudit> result = auditService.getAuditsByActionType("TAKEN");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(medicationAuditRepository, times(1)).findByActionType("TAKEN");
    }
}
