package com.medreminder.auditservice.repository;

import com.medreminder.auditservice.entity.MedicationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationAuditRepository extends JpaRepository<MedicationAudit, UUID> {

    List<MedicationAudit> findByPatientId(UUID patientId);

    List<MedicationAudit> findByAction(String action);

    List<MedicationAudit> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    @Query("SELECT ma FROM MedicationAudit ma WHERE ma.patientId = ?1 AND ma.createdAt >= ?2 AND ma.createdAt <= ?3 ORDER BY ma.createdAt DESC")
    List<MedicationAudit> findAuditsByDateRange(UUID patientId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT ma FROM MedicationAudit ma WHERE ma.patientId = ?1 AND ma.status = 'MISSED' ORDER BY ma.createdAt DESC")
    List<MedicationAudit> findMissedMedications(UUID patientId);

    @Query("SELECT COUNT(ma) FROM MedicationAudit ma WHERE ma.patientId = ?1 AND ma.status = 'TAKEN' AND ma.createdAt >= ?2 AND ma.createdAt <= ?3")
    int countMedicationsTaken(UUID patientId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COUNT(ma) FROM MedicationAudit ma WHERE ma.patientId = ?1 AND ma.status = 'SCHEDULED' AND ma.createdAt >= ?2 AND ma.createdAt <= ?3")
    int countMedicationsScheduled(UUID patientId, LocalDateTime startDate, LocalDateTime endDate);
}
