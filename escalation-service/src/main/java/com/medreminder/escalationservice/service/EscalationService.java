package com.medreminder.escalationservice.service;

import com.medreminder.common.dto.DoseMissedEvent;
import com.medreminder.escalationservice.dto.EscalationRequest;
import com.medreminder.escalationservice.dto.EscalationResponse;
import com.medreminder.escalationservice.entity.Escalation;
import com.medreminder.escalationservice.exception.EscalationException;
import com.medreminder.escalationservice.repository.EscalationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
public class EscalationService {

    @Autowired
    private EscalationRepository escalationRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Transactional
    public EscalationResponse triggerEscalation(EscalationRequest request) {
        log.info("Triggering escalation for patient: {}, type: {}", request.getPatientId(), request.getEscalationType());

        try {
            Escalation escalation = new Escalation();
            escalation.setPatientId(request.getPatientId());
            escalation.setScheduleId(request.getScheduleId());
            escalation.setEscalationType(request.getEscalationType());
            escalation.setStatus("TRIGGERED");
            escalation.setEscalationNotes("Medication: " + request.getMedicationName());

            Escalation savedEscalation = escalationRepository.save(escalation);
            log.info("Escalation created with ID: {}", savedEscalation.getEscalationId());

            try {
                rabbitTemplate.convertAndSend("med-reminder-exchange", "escalation.route", savedEscalation);
                log.info("Escalation message sent to RabbitMQ");
            } catch (Exception e) {
                log.warn("Failed to send escalation to RabbitMQ: {}", e.getMessage());
            }

            return mapToResponse(savedEscalation);

        } catch (Exception e) {
            log.error("Error triggering escalation for patient: {}", request.getPatientId(), e);
            throw new EscalationException("Failed to trigger escalation: " + e.getMessage(), e);
        }
    }

    @KafkaListener(topics = com.medreminder.common.util.Constants.KAFKA_TOPIC_DOSE_MISSED, groupId = "escalation-service-group")
    @Transactional
    public void handleDoseMissedEvent(DoseMissedEvent event) {
        log.info("Received dose-missed event for patient: {}", event.getPatientId());

        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long missedDoseCount = escalationRepository.countMissedDosesInRange(
                    event.getPatientId(),
                    oneHourAgo
            );

            String escalationType = missedDoseCount > 2 ? "REPEATED_MISSED" : "MISSED_DOSE";

            EscalationRequest request = new EscalationRequest();
            request.setPatientId(event.getPatientId());
            request.setScheduleId(event.getScheduleId());
            request.setEscalationType(escalationType);
            request.setMedicationName(event.getMedicationId() != null ? event.getMedicationId().toString() : "UNKNOWN");

            triggerEscalation(request);

        } catch (Exception e) {
            log.error("Error processing dose-missed event: {}", e.getMessage(), e);
        }
    }

    public EscalationResponse getEscalation(UUID escalationId) {
        Escalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new EscalationException("Escalation not found: " + escalationId));
        return mapToResponse(escalation);
    }

    public List<EscalationResponse> getPatientEscalations(UUID patientId) {
        List<Escalation> escalations = escalationRepository.findByPatientId(patientId);
        return escalations.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public EscalationResponse resolveEscalation(UUID escalationId) {
        Escalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new EscalationException("Escalation not found: " + escalationId));

        escalation.setStatus("COMPLETED");
        escalation.setResolvedAt(LocalDateTime.now());
        Escalation updated = escalationRepository.save(escalation);

        return mapToResponse(updated);
    }

    private EscalationResponse mapToResponse(Escalation escalation) {
        return new EscalationResponse(
                escalation.getEscalationId(),
                escalation.getPatientId(),
                escalation.getScheduleId(),
                escalation.getEscalationType(),
                escalation.getStatus(),
                escalation.getTriggeredAt(),
                escalation.getNotifiedAt()
        );
    }
}
