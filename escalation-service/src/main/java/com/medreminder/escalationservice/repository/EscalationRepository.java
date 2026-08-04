package com.medreminder.escalationservice.repository;

import com.medreminder.escalationservice.entity.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, UUID> {

    List<Escalation> findByPatientId(UUID patientId);

    @Query("SELECT e FROM Escalation e WHERE e.status = 'TRIGGERED' ORDER BY e.triggeredAt ASC")
    List<Escalation> findPendingEscalations();

    @Query("SELECT COUNT(e) FROM Escalation e WHERE e.patientId = ?1 AND e.escalationType = 'MISSED_DOSE' AND e.triggeredAt >= ?2")
    long countMissedDosesInRange(UUID patientId, LocalDateTime fromDateTime);

    List<Escalation> findByPatientIdOrderByCreatedAtDesc(java.util.UUID patientId);

}
