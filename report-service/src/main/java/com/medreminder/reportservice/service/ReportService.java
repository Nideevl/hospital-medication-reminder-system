package com.medreminder.reportservice.service;

import com.medreminder.reportservice.dto.MedicationReportRequest;
import com.medreminder.reportservice.dto.MedicationReportResponse;
import com.medreminder.reportservice.entity.ComplianceData;
import com.medreminder.reportservice.entity.MedicationReport;
import com.medreminder.reportservice.repository.ComplianceDataRepository;
import com.medreminder.reportservice.repository.MedicationReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportService {

    @Autowired
    private MedicationReportRepository medicationReportRepository;

    @Autowired
    private ComplianceDataRepository complianceDataRepository;

    @Transactional
    public MedicationReportResponse generateReport(MedicationReportRequest request) {
        log.info("Generating {} report for patient: {}", request.getReportType(), request.getPatientId());

        // Calculate compliance metrics (in real scenario, fetch from audit service)
        int totalScheduled = calculateTotalScheduled(request.getPatientId(), request.getStartDate(), request.getEndDate());
        int totalTaken = calculateTotalTaken(request.getPatientId(), request.getStartDate(), request.getEndDate());
        int totalMissed = totalScheduled - totalTaken;
        double compliancePercentage = totalScheduled > 0 ? (double) totalTaken / totalScheduled * 100 : 0;

        MedicationReport report = new MedicationReport();
        report.setPatientId(request.getPatientId());
        report.setReportType(request.getReportType());
        report.setStartDate(request.getStartDate());
        report.setEndDate(request.getEndDate());
        report.setTotalScheduled(totalScheduled);
        report.setTotalTaken(totalTaken);
        report.setTotalMissed(totalMissed);
        report.setCompliancePercentage(compliancePercentage);
        report.setReportStatus("COMPLETED");
        report.setGeneratedBy("SYSTEM");
        report.setSummary(generateSummary(totalScheduled, totalTaken, compliancePercentage));

        MedicationReport saved = medicationReportRepository.save(report);
        log.info("Report generated with ID: {} - Compliance: {}%", saved.getReportId(), compliancePercentage);

        return mapToResponse(saved);
    }

    @Scheduled(cron = "0 0 * * * ?")  // Daily at midnight
    @Transactional
    public void generateDailyComplianceReports() {
        log.info("Generating daily compliance reports");
        // Fetch all active patients and generate their compliance data
        // This would typically fetch from patient service
    }

    public List<MedicationReportResponse> getPatientReports(UUID patientId) {
        List<MedicationReport> reports = medicationReportRepository.findByPatientId(patientId);
        return reports.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public MedicationReportResponse getReport(UUID reportId) {
        MedicationReport report = medicationReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        return mapToResponse(report);
    }

    public List<ComplianceData> getPatientComplianceData(UUID patientId) {
        return complianceDataRepository.findByPatientIdOrderByReportDateDesc(patientId);
    }

    private int calculateTotalScheduled(UUID patientId, LocalDateTime startDate, LocalDateTime endDate) {
        // Placeholder: In real scenario, query audit service
        return 30;
    }

    private int calculateTotalTaken(UUID patientId, LocalDateTime startDate, LocalDateTime endDate) {
        // Placeholder: In real scenario, query audit service
        return 27;
    }

    private String generateSummary(int totalScheduled, int totalTaken, double compliancePercentage) {
        return String.format(
                "During this period, %d medications were scheduled. %d were taken successfully (%.1f%% compliance). " +
                "The patient demonstrated %s adherence to their medication schedule.",
                totalScheduled,
                totalTaken,
                compliancePercentage,
                compliancePercentage >= 80 ? "excellent" : compliancePercentage >= 60 ? "good" : "needs improvement"
        );
    }

    private MedicationReportResponse mapToResponse(MedicationReport report) {
        return new MedicationReportResponse(
                report.getReportId(),
                report.getPatientId(),
                report.getReportType(),
                report.getTotalScheduled(),
                report.getTotalTaken(),
                report.getTotalMissed(),
                report.getCompliancePercentage(),
                report.getGeneratedAt()
        );
    }
}
