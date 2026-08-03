package com.medreminder.reportservice.controller;

import com.medreminder.reportservice.dto.MedicationReportRequest;
import com.medreminder.reportservice.dto.MedicationReportResponse;
import com.medreminder.reportservice.entity.ComplianceData;
import com.medreminder.reportservice.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/generate")
    public ResponseEntity<MedicationReportResponse> generateReport(@RequestBody MedicationReportRequest request) {
        log.info("POST /api/reports/generate - Patient: {}, Type: {}", request.getPatientId(), request.getReportType());
        MedicationReportResponse response = reportService.generateReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<MedicationReportResponse> getReport(@PathVariable UUID reportId) {
        log.info("GET /api/reports/{}", reportId);
        return ResponseEntity.ok(reportService.getReport(reportId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicationReportResponse>> getPatientReports(@PathVariable UUID patientId) {
        log.info("GET /api/reports/patient/{}", patientId);
        return ResponseEntity.ok(reportService.getPatientReports(patientId));
    }

    @GetMapping("/patient/{patientId}/compliance")
    public ResponseEntity<List<ComplianceData>> getPatientComplianceData(@PathVariable UUID patientId) {
        log.info("GET /api/reports/patient/{}/compliance", patientId);
        return ResponseEntity.ok(reportService.getPatientComplianceData(patientId));
    }
}
