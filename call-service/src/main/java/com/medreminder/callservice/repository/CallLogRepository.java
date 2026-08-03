package com.medreminder.callservice.repository;

import com.medreminder.callservice.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, UUID> {
    Optional<CallLog> findByCallId(UUID callId);
    List<CallLog> findByPatientId(UUID patientId);

    @Query("SELECT c FROM CallLog c WHERE c.scheduleId = ?1 ORDER BY c.callInitiatedAt DESC LIMIT 1")
    Optional<CallLog> findLatestByScheduleId(UUID scheduleId);

    @Query("SELECT c FROM CallLog c WHERE c.patientId = ?1 AND c.callInitiatedAt >= ?2")
    List<CallLog> findCallsAfter(UUID patientId, LocalDateTime fromDateTime);
}
