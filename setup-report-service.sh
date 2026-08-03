#!/bin/bash

# Report Service Generator
# Run this in your project root: bash setup-report-service.sh

set -e

PROJECT_ROOT="$(pwd)"
echo "Setting up Report Service in: $PROJECT_ROOT"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Creating Report Service...${NC}"

mkdir -p report-service/src/main/java/com/medreminder/reportservice/{entity,repository,service,controller,dto,config}
mkdir -p report-service/src/main/resources

# ReportServiceApplication.java
cat > report-service/src/main/java/com/medreminder/reportservice/ReportServiceApplication.java << 'JAVA'
package com.medreminder.reportservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.medreminder.reportservice", "com.medreminder.common"})
public class ReportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}
JAVA

# MedicationReport.java
cat > report-service/src/main/java/com/medreminder/reportservice/entity/MedicationReport.java << 'JAVA'
package com.medreminder.reportservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "medication_reports")
public class MedicationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reportId;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private String reportType;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Integer totalScheduled;

    @Column(nullable = false)
    private Integer totalTaken;

    @Column(nullable = false)
    private Integer totalMissed;

    @Column
    private Double compliancePercentage;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime generatedAt;

    @Column
    private String generatedBy;

    @Column
    private String reportStatus;
}
JAVA

# ComplianceData.java
cat > report-service/src/main/java/com/medreminder/reportservice/entity/ComplianceData.java << 'JAVA'
package com.medreminder.reportservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "compliance_data")
public class ComplianceData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID complianceId;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private LocalDate reportDate;

    @Column(nullable = false)
    private Integer medicationsScheduled;

    @Column(nullable = false)
    private Integer medicationsTaken;

    @Column(nullable = false)
    private Integer medicationsMissed;

    @Column
    private Double dailyCompliancePercentage;

    @Column
    private String notes;
}
JAVA

# MedicationReportRepository.java
cat > report-service/src/main/java/com/medreminder/reportservice/repository/MedicationReportRepository.java << 'JAVA'
package com.medreminder.reportservice.repository;

import com.medreminder.reportservice.entity.MedicationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationReportRepository extends JpaRepository<MedicationReport, UUID> {

    List<MedicationReport> findByPatientId(UUID patientId);

    @Query("SELECT mr FROM MedicationReport mr WHERE mr.patientId = ?1 AND mr.startDate >= ?2 AND mr.endDate <= ?3")
    List<MedicationReport> findReportsInDateRange(UUID patientId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT mr FROM MedicationReport mr WHERE mr.reportStatus = 'COMPLETED' ORDER BY mr.generatedAt DESC")
    List<MedicationReport> findCompletedReports();
}
JAVA

# ComplianceDataRepository.java
cat > report-service/src/main/java/com/medreminder/reportservice/repository/ComplianceDataRepository.java << 'JAVA'
package com.medreminder.reportservice.repository;

import com.medreminder.reportservice.entity.ComplianceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComplianceDataRepository extends JpaRepository<ComplianceData, UUID> {

    List<ComplianceData> findByPatientIdOrderByReportDateDesc(UUID patientId);

    @Query("SELECT cd FROM ComplianceData cd WHERE cd.patientId = ?1 AND cd.reportDate >= ?2 AND cd.reportDate <= ?3")
    List<ComplianceData> findComplianceInDateRange(UUID patientId, LocalDate fromDate, LocalDate toDate);
}
JAVA

# MedicationReportRequest.java
cat > report-service/src/main/java/com/medreminder/reportservice/dto/MedicationReportRequest.java << 'JAVA'
package com.medreminder.reportservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReportRequest {
    private UUID patientId;
    private String reportType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
JAVA

# MedicationReportResponse.java
cat > report-service/src/main/java/com/medreminder/reportservice/dto/MedicationReportResponse.java << 'JAVA'
package com.medreminder.reportservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReportResponse {
    private UUID reportId;
    private UUID patientId;
    private String reportType;
    private Integer totalScheduled;
    private Integer totalTaken;
    private Integer totalMissed;
    private Double compliancePercentage;
    private LocalDateTime generatedAt;
}
JAVA

# ReportService.java
cat > report-service/src/main/java/com/medreminder/reportservice/service/ReportService.java << 'JAVA'
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
JAVA

# ReportController.java
cat > report-service/src/main/java/com/medreminder/reportservice/controller/ReportController.java << 'JAVA'
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
JAVA

# application.yml
cat > report-service/src/main/resources/application.yml << 'YML'
spring:
  application:
    name: report-service
  datasource:
    url: jdbc:postgresql://localhost:5432/med_reminder
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 20
  jpa:
    hibernate:
      ddl-auto: validate

server:
  port: 8086

logging:
  level:
    root: INFO
    com.medreminder: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
YML

# pom.xml
cat > report-service/pom.xml << 'XML'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.medreminder</groupId>
        <artifactId>hospital-medication-reminder-system</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>report-service</artifactId>
    <name>Report Service</name>

    <dependencies>
        <dependency>
            <groupId>com.medreminder</groupId>
            <artifactId>common-module</artifactId>
            <version>1.0.0</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
XML

echo -e "${GREEN}✓ Report Service created${NC}"

