package com.medreminder.reportservice.repository;

import com.medreminder.reportservice.entity.ComplianceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceDataRepository extends JpaRepository<ComplianceData, UUID> {

    List<ComplianceData> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    Optional<ComplianceData> findByPatientIdAndWeekNumber(UUID patientId, Integer weekNumber);

    @Query("SELECT cd FROM ComplianceData cd WHERE cd.patientId = ?1 AND cd.createdAt >= ?2 AND cd.createdAt <= ?3")
    List<ComplianceData> findComplianceInDateRange(UUID patientId, LocalDateTime fromDate, LocalDateTime toDate);
}