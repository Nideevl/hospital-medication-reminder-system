package com.medreminder.auditservice.service;

import com.medreminder.auditservice.entity.MedicationAudit;
import com.medreminder.auditservice.repository.MedicationAuditRepository;
import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.dto.DoseMissedEvent;
import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final MedicationAuditRepository medicationAuditRepository;

    @Autowired
    public AuditEventConsumer(MedicationAuditRepository medicationAuditRepository) {
        this.medicationAuditRepository = medicationAuditRepository;
    }

    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_MEDICATION_DUE,
        groupId = "${spring.kafka.consumer.group-id:audit-service-group}"
    )
    @Transactional
    public void handleMedicationDueEvent(@Payload MedicationDueEvent event) {
        log.info("Received medication-due event for audit: patientId={}, medicationId={}",
                event.getPatientId(), event.getMedicationId());

        try {
            MedicationAudit audit = new MedicationAudit();
            audit.setPatientId(event.getPatientId());
            audit.setScheduleId(event.getMedicationId());
            audit.setMedicationName("Scheduled Medication");
            audit.setAction("MEDICATION_DUE");
            audit.setStatus("PENDING");
            if (event.getScheduledTime() != null) {
                audit.setScheduledTime(LocalDateTime.now().with(event.getScheduledTime()));
            }
            audit.setRecordedBy("SYSTEM");

            medicationAuditRepository.save(audit);
            log.info("Audit record created for medication-due event: auditId={}", audit.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit for medication-due event: patientId={}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_CALL_RESPONSE,
        groupId = "${spring.kafka.consumer.group-id:audit-service-group}"
    )
    @Transactional
    public void handleCallResponseEvent(@Payload CallResponseEvent event) {
        log.info("Received call-response event for audit: patientId={}, callLogId={}",
                event.getPatientId(), event.getCallLogId());

        try {
            MedicationAudit audit = new MedicationAudit();
            audit.setPatientId(event.getPatientId());
            audit.setScheduleId(event.getScheduleId());
            audit.setMedicationName("N/A");
            audit.setAction(event.isResponseReceived() ? "CALL_RESPONSE_TAKEN" : "CALL_RESPONSE_MISSED");
            audit.setStatus(event.getCallStatus() != null ? event.getCallStatus() : "COMPLETED");
            audit.setActualTime(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());
            audit.setNotes("IVR Response: " + event.getIvrResponse());
            audit.setRecordedBy("CALL_SERVICE");

            medicationAuditRepository.save(audit);
            log.info("Audit record created for call-response event: auditId={}", audit.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit for call-response event: patientId={}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_DOSE_MISSED,
        groupId = "${spring.kafka.consumer.group-id:audit-service-group}"
    )
    @Transactional
    public void handleDoseMissedEvent(@Payload DoseMissedEvent event) {
        log.info("Received dose-missed event for audit: patientId={}, scheduleId={}",
                event.getPatientId(), event.getScheduleId());

        try {
            MedicationAudit audit = new MedicationAudit();
            audit.setPatientId(event.getPatientId());
            audit.setScheduleId(event.getScheduleId());
            audit.setMedicationName("N/A");
            audit.setAction("DOSE_MISSED");
            audit.setStatus("MISSED");
            audit.setActualTime(event.getMissedTime() != null ? event.getMissedTime() : LocalDateTime.now());
            audit.setRecordedBy("ESCALATION_SERVICE");

            medicationAuditRepository.save(audit);
            log.info("Audit record created for dose-missed event: auditId={}", audit.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit for dose-missed event: patientId={}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }
}
