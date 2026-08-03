package com.medreminder.reportservice.service;

import com.medreminder.reportservice.entity.MedicationReport;
import com.medreminder.reportservice.exception.ResourceNotFoundException;
import com.medreminder.reportservice.repository.MedicationReportRepository;
import com.medreminder.reportservice.repository.ComplianceDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("Report Service Unit Tests")
class ReportServiceTest {

    @Mock
    private MedicationReportRepository medicationReportRepository;

    @Mock
    private ComplianceDataRepository complianceDataRepository;

    @InjectMocks
    private ReportService reportService;

    private MedicationReport testReport;
    private UUID reportId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        reportId = UUID.randomUUID();
        patientId = UUID.randomUUID();

        testReport = MedicationReport.builder()
                .reportId(reportId)
                .patientId(patientId)
                .hospitalId("HOSP001")
                .reportDate(LocalDate.now())
                .totalMedicationsScheduled(10)
                .medicationsTaken(7)
                .medicationsMissed(3)
                .adherencePercentage(new BigDecimal("70.00"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should generate report successfully")
    void testGenerateReport_Success() {
        // Arrange
        when(medicationReportRepository.save(any(MedicationReport.class))).thenReturn(testReport);

        // Act
        MedicationReport result = reportService.generateReport(patientId, "HOSP001", LocalDate.now());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getReportId()).isEqualTo(reportId);
        assertThat(result.getAdherencePercentage()).isEqualByComparingTo(new BigDecimal("70.00"));
        verify(medicationReportRepository, times(1)).save(any(MedicationReport.class));
    }

    @Test
    @DisplayName("Should calculate adherence percentage correctly")
    void testCalculateAdherence_Success() {
        // Act
        BigDecimal result = reportService.calculateAdherencePercentage(7, 10);

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("Should return zero adherence when total is zero")
    void testCalculateAdherence_ZeroTotal() {
        // Act
        BigDecimal result = reportService.calculateAdherencePercentage(0, 0);

        // Assert
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should retrieve compliance data successfully")
    void testGetComplianceData_Success() {
        // Arrange
        when(complianceDataRepository.findByPatientId(patientId))
                .thenReturn(Arrays.asList());

        // Act
        List result = reportService.getComplianceData(patientId);

        // Assert
        assertThat(result).isNotNull();
        verify(complianceDataRepository, times(1)).findByPatientId(patientId);
    }

    @Test
    @DisplayName("Should generate monthly report successfully")
    void testGenerateMonthlyReport_Success() {
        // Arrange
        when(medicationReportRepository.findByPatientIdAndReportDateBetween(any(), any(), any()))
                .thenReturn(Arrays.asList(testReport));

        // Act
        MedicationReport result = reportService.generateMonthlyReport(patientId, "2024-01");

        // Assert
        assertThat(result).isNotNull();
        verify(medicationReportRepository, times(1))
                .findByPatientIdAndReportDateBetween(any(), any(), any());
    }

    @Test
    @DisplayName("Should retrieve report by ID successfully")
    void testGetReportById_Success() {
        // Arrange
        when(medicationReportRepository.findById(reportId)).thenReturn(Optional.of(testReport));

        // Act
        MedicationReport result = reportService.getReportById(reportId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getReportId()).isEqualTo(reportId);
        verify(medicationReportRepository, times(1)).findById(reportId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when report not found")
    void testGetReportById_NotFound() {
        // Arrange
        when(medicationReportRepository.findById(reportId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reportService.getReportById(reportId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found with ID: " + reportId);
    }
}
