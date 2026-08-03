package com.medreminder.auditservice.service;

import com.medreminder.auditservice.dto.MedicationAuditDTO;
import com.medreminder.auditservice.dto.RecordMedicationRequest;
import com.medreminder.auditservice.dto.UserActionDTO;
import com.medreminder.auditservice.entity.MedicationAudit;
import com.medreminder.auditservice.entity.UserAction;
import com.medreminder.auditservice.repository.MedicationAuditRepository;
import com.medreminder.auditservice.repository.UserActionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuditService {

    @Autowired
    private MedicationAuditRepository medicationAuditRepository;

    @Autowired
    private UserActionRepository userActionRepository;

    @Transactional
    public MedicationAuditDTO recordMedicationEvent(RecordMedicationRequest request) {
        log.info("Recording medication event - Patient: {}, Medication: {}, Status: {}", 
                request.getPatientId(), request.getMedicationName(), request.getStatus());

        MedicationAudit audit = new MedicationAudit();
        audit.setPatientId(request.getPatientId());
        audit.setScheduleId(request.getScheduleId());
        audit.setMedicationName(request.getMedicationName());
        audit.setAction(request.getAction());
        audit.setStatus(request.getStatus());
        audit.setScheduledTime(request.getScheduledTime());
        audit.setActualTime(request.getActualTime());
        audit.setNotes(request.getNotes());

        // Calculate delay if medication was taken
        if (request.getActualTime() != null && request.getScheduledTime() != null) {
            long delayMinutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    request.getScheduledTime(), request.getActualTime()
            );
            audit.setDelayMinutes(delayMinutes);
        }

        MedicationAudit saved = medicationAuditRepository.save(audit);
        log.info("Medication event recorded with ID: {}", saved.getAuditId());

        return mapToDTO(saved);
    }

    @KafkaListener(topics = "medication-taken", groupId = "audit-service-group")
    @Transactional
    public void handleMedicationTakenEvent(String event) {
        log.info("Processing medication-taken event: {}", event);
        // Parse event and record in database
    }

    public MedicationAuditDTO getMedicationAudit(UUID auditId) {
        MedicationAudit audit = medicationAuditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit record not found"));
        return mapToDTO(audit);
    }

    public List<MedicationAuditDTO> getPatientAuditHistory(UUID patientId) {
        List<MedicationAudit> audits = medicationAuditRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return audits.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<MedicationAuditDTO> getAuditsByDateRange(UUID patientId, LocalDateTime startDate, LocalDateTime endDate) {
        List<MedicationAudit> audits = medicationAuditRepository.findAuditsByDateRange(patientId, startDate, endDate);
        return audits.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<MedicationAuditDTO> getMissedMedications(UUID patientId) {
        List<MedicationAudit> audits = medicationAuditRepository.findMissedMedications(patientId);
        return audits.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public void recordUserAction(UUID userId, String actionType, String entity, UUID entityId, String changeDetails) {
        log.info("Recording user action - User: {}, Action: {}, Entity: {}", userId, actionType, entity);

        UserAction action = new UserAction();
        action.setUserId(userId);
        action.setActionType(actionType);
        action.setEntity(entity);
        action.setEntityId(entityId);
        action.setChangeDetails(changeDetails);

        userActionRepository.save(action);
    }

    public List<UserActionDTO> getUserActions(UUID userId) {
        List<UserAction> actions = userActionRepository.findByUserIdOrderByTimestampDesc(userId);
        return actions.stream().map(this::mapUserActionToDTO).collect(Collectors.toList());
    }

    private MedicationAuditDTO mapToDTO(MedicationAudit audit) {
        return new MedicationAuditDTO(
                audit.getAuditId(),
                audit.getPatientId(),
                audit.getScheduleId(),
                audit.getMedicationName(),
                audit.getAction(),
                audit.getStatus(),
                audit.getScheduledTime(),
                audit.getActualTime(),
                audit.getDelayMinutes(),
                audit.getNotes(),
                audit.getCreatedAt()
        );
    }

    private UserActionDTO mapUserActionToDTO(UserAction action) {
        return new UserActionDTO(
                action.getActionId(),
                action.getUserId(),
                action.getActionType(),
                action.getEntity(),
                action.getEntityId(),
                action.getChangeDetails(),
                action.getTimestamp()
        );
    }
}
