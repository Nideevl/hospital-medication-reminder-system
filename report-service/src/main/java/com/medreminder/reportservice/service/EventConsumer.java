package com.medreminder.reportservice.service;

import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.dto.DoseMissedEvent;
import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.reportservice.entity.ComplianceData;
import com.medreminder.reportservice.entity.MedicationReport;
import com.medreminder.reportservice.repository.ComplianceDataRepository;
import com.medreminder.reportservice.repository.MedicationReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Kafka Consumer for Report Service.
 * Listens for medication-due, call-response-received, and dose-missed events
 * to build compliance reports.
 */
@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final MedicationReportRepository medicationReportRepository;
    private final ComplianceDataRepository complianceDataRepository;

    @Autowired
    public EventConsumer(MedicationReportRepository medicationReportRepository,
                         ComplianceDataRepository complianceDataRepository) {
        this.medicationReportRepository = medicationReportRepository;
        this.complianceDataRepository = complianceDataRepository;
    }

    /**
     * Handles medication-due events from Kafka.
     * Updates medication report with scheduled medications count.
     *
     * @param event MedicationDueEvent from Kafka
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_MEDICATION_DUE,
        groupId = "report-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMedicationDueEvent(@Payload MedicationDueEvent event) {
        log.info("Received medication-due event: patientId={}, medicationId={}, scheduledTime={}",
                event.getPatientId(), event.getMedicationId(), event.getScheduledTime());

        try {
            UUID patientId = UUID.fromString(event.getPatientId());
            LocalDate reportDate = LocalDate.now();

            // Get or create medication report for today
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, reportDate)
                    .orElseGet(() -> createNewReport(patientId, reportDate));

            // Increment total scheduled medications
            report.setTotalMedicationsScheduled(report.getTotalMedicationsScheduled() + 1);
            updateAdherencePercentage(report);
            
            medicationReportRepository.save(report);

            log.info("Successfully processed medication-due event for patientId: {}, totalScheduled: {}",
                    patientId, report.getTotalMedicationsScheduled());

        } catch (Exception e) {
            log.error("Error processing medication-due event for patientId: {}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Handles call-response-received events from Kafka.
     * Updates medication report with taken medications count.
     *
     * @param event CallResponseEvent from Kafka
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_CALL_RESPONSE,
        groupId = "report-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCallResponseEvent(@Payload CallResponseEvent event) {
        log.info("Received call-response-received event: patientId={}, responseReceived={}",
                event.getPatientId(), event.isResponseReceived());

        try {
            UUID patientId = UUID.fromString(event.getPatientId());
            LocalDate reportDate = LocalDate.now();

            // Get or create medication report for today
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, reportDate)
                    .orElseGet(() -> createNewReport(patientId, reportDate));

            // Update taken count if response was received
            if (event.isResponseReceived()) {
                report.setMedicationsTaken(report.getMedicationsTaken() + 1);
                updateAdherencePercentage(report);
                medicationReportRepository.save(report);

                // Update compliance data for the week/month
                updateComplianceData(patientId, reportDate, true);
            }

            log.info("Successfully processed call-response event for patientId: {}, taken: {}",
                    patientId, event.isResponseReceived());

        } catch (Exception e) {
            log.error("Error processing call-response event for patientId: {}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Handles dose-missed events from Kafka.
     * Updates medication report with missed medications count.
     *
     * @param event DoseMissedEvent from Kafka
     */
    @KafkaListener(
        topics = Constants.KAFKA_TOPIC_DOSE_MISSED,
        groupId = "report-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDoseMissedEvent(@Payload DoseMissedEvent event) {
        log.info("Received dose-missed event: patientId={}, scheduleId={}, missedTime={}",
                event.getPatientId(), event.getScheduleId(), event.getMissedTime());

        try {
            UUID patientId = UUID.fromString(event.getPatientId());
            LocalDate reportDate = LocalDate.now();

            // Get or create medication report for today
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, reportDate)
                    .orElseGet(() -> createNewReport(patientId, reportDate));

            // Increment missed medications
            report.setMedicationsMissed(report.getMedicationsMissed() + 1);
            updateAdherencePercentage(report);
            medicationReportRepository.save(report);

            // Update compliance data for the week/month with missed dose
            updateComplianceData(patientId, reportDate, false);

            log.info("Successfully processed dose-missed event for patientId: {}, missed: {}",
                    patientId, report.getMedicationsMissed());

        } catch (Exception e) {
            log.error("Error processing dose-missed event for patientId: {}, error: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Creates a new MedicationReport for a patient for a specific date.
     *
     * @param patientId UUID of patient
     * @param reportDate Date for the report
     * @return New MedicationReport entity
     */
    private MedicationReport createNewReport(UUID patientId, LocalDate reportDate) {
        MedicationReport report = new MedicationReport();
        report.setPatientId(patientId);
        report.setReportDate(reportDate);
        report.setTotalMedicationsScheduled(0);
        report.setMedicationsTaken(0);
        report.setMedicationsMissed(0);
        report.setAdherencePercentage(BigDecimal.ZERO);
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }

    /**
     * Updates compliance data for a patient for the current week and month.
     *
     * @param patientId UUID of patient
     * @param date Date for compliance data
     * @param doseTaken Whether the dose was taken
     */
    private void updateComplianceData(UUID patientId, LocalDate date, boolean doseTaken) {
        try {
            // Update weekly compliance
            int weekNumber = date.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
            String month = date.getMonth().toString();

            Optional<ComplianceData> existingWeekly = complianceDataRepository
                    .findByPatientIdAndWeekNumber(patientId, weekNumber);

            ComplianceData compliance;
            if (existingWeekly.isPresent()) {
                compliance = existingWeekly.get();
            } else {
                compliance = new ComplianceData();
                compliance.setPatientId(patientId);
                compliance.setWeekNumber(weekNumber);
                compliance.setMonth(month);
                compliance.setAdherenceScore(BigDecimal.ZERO);
                compliance.setMissedDoses(0);
                compliance.setEscalationsTriggered(0);
                compliance.setCreatedAt(LocalDateTime.now());
            }

            if (!doseTaken) {
                compliance.setMissedDoses(compliance.getMissedDoses() + 1);
            }

            // Recalculate adherence score
            MedicationReport report = medicationReportRepository
                    .findByPatientIdAndReportDate(patientId, date)
                    .orElse(null);

            if (report != null) {
                int total = report.getTotalMedicationsScheduled();
                int taken = report.getMedicationsTaken();
                if (total > 0) {
                    BigDecimal score = BigDecimal.valueOf((double) taken / total * 100)
                            .setScale(2, RoundingMode.HALF_UP);
                    compliance.setAdherenceScore(score);
                }
            }

            complianceDataRepository.save(compliance);
            log.debug("Updated compliance data for patientId: {}, week: {}, month: {}",
                    patientId, weekNumber, month);

        } catch (Exception e) {
            log.error("Error updating compliance data for patientId: {}, error: {}",
                    patientId, e.getMessage(), e);
        }
    }

    /**
     * Updates the adherence percentage for a medication report.
     *
     * @param report MedicationReport to update
     */
    private void updateAdherencePercentage(MedicationReport report) {
        int total = report.getTotalMedicationsScheduled();
        int taken = report.getMedicationsTaken();
        
        if (total > 0) {
            BigDecimal percentage = BigDecimal.valueOf((double) taken / total * 100)
                    .setScale(2, RoundingMode.HALF_UP);
            report.setAdherencePercentage(percentage);
        } else {
            report.setAdherencePercentage(BigDecimal.ZERO);
        }
    }
}
