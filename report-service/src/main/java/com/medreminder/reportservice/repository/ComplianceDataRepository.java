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
