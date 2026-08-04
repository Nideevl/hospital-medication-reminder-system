package com.medreminder.reportservice.repository;

import com.medreminder.reportservice.entity.MedicationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationReportRepository extends JpaRepository<MedicationReport, UUID> {

    List<MedicationReport> findByPatientId(UUID patientId);

    Optional<MedicationReport> findByPatientIdAndReportDate(UUID patientId, LocalDate reportDate);

    @Query("SELECT mr FROM MedicationReport mr WHERE mr.patientId = ?1 AND mr.startDate >= ?2 AND mr.endDate <= ?3")
    List<MedicationReport> findReportsInDateRange(UUID patientId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT mr FROM MedicationReport mr WHERE mr.reportStatus = 'COMPLETED' ORDER BY mr.generatedAt DESC")
    List<MedicationReport> findCompletedReports();
}
