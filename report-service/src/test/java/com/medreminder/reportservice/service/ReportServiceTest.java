package com.medreminder.reportservice.service;

import com.medreminder.reportservice.dto.MedicationReportRequest;
import com.medreminder.reportservice.dto.MedicationReportResponse;
import com.medreminder.reportservice.entity.ComplianceData;
import com.medreminder.reportservice.entity.MedicationReport;
import com.medreminder.reportservice.repository.ComplianceDataRepository;
import com.medreminder.reportservice.repository.MedicationReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
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

        testReport = new MedicationReport();
        testReport.setReportId(reportId);
        testReport.setPatientId(patientId);
        testReport.setReportType("WEEKLY");
        testReport.setTotalScheduled(30);
        testReport.setTotalTaken(27);
        testReport.setTotalMissed(3);
        testReport.setCompliancePercentage(90.0);
        testReport.setReportStatus("COMPLETED");
        testReport.setGeneratedBy("SYSTEM");
        testReport.setGeneratedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should generate report successfully")
    void testGenerateReport_Success() {
        when(medicationReportRepository.save(any(MedicationReport.class))).thenReturn(testReport);

        MedicationReportRequest request = new MedicationReportRequest();
        request.setPatientId(patientId);
        request.setReportType("WEEKLY");
        request.setStartDate(LocalDateTime.now().minusDays(7));
        request.setEndDate(LocalDateTime.now());

        MedicationReportResponse response = reportService.generateReport(request);

        assertThat(response).isNotNull();
        assertThat(response.getReportId()).isEqualTo(reportId);
        assertThat(response.getCompliancePercentage()).isEqualTo(90.0);
        verify(medicationReportRepository, times(1)).save(any(MedicationReport.class));
    }

    @Test
    @DisplayName("Should retrieve patient reports successfully")
    void testGetPatientReports_Success() {
        when(medicationReportRepository.findByPatientId(patientId))
                .thenReturn(Collections.singletonList(testReport));

        List<MedicationReportResponse> reports = reportService.getPatientReports(patientId);

        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getReportId()).isEqualTo(reportId);
        verify(medicationReportRepository, times(1)).findByPatientId(patientId);
    }

    @Test
    @DisplayName("Should retrieve report by ID successfully")
    void testGetReport_Success() {
        when(medicationReportRepository.findById(reportId)).thenReturn(Optional.of(testReport));

        MedicationReportResponse response = reportService.getReport(reportId);

        assertThat(response).isNotNull();
        assertThat(response.getReportId()).isEqualTo(reportId);
        verify(medicationReportRepository, times(1)).findById(reportId);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when report not found")
    void testGetReport_NotFound() {
        when(medicationReportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReport(reportId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Report not found");
    }

    @Test
    @DisplayName("Should retrieve patient compliance data successfully")
    void testGetPatientComplianceData_Success() {
        ComplianceData sampleCompliance = ComplianceData.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .weekNumber(1)
                .adherenceScore(95.0)
                .createdAt(LocalDateTime.now())
                .build();

        List<ComplianceData> mockData = Collections.singletonList(sampleCompliance);

        when(complianceDataRepository.findByPatientIdOrderByCreatedAtDesc(patientId))
                .thenReturn(mockData);

        List<ComplianceData> result = reportService.getPatientComplianceData(patientId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientId()).isEqualTo(patientId);
        verify(complianceDataRepository, times(1)).findByPatientIdOrderByCreatedAtDesc(patientId);
    }
}