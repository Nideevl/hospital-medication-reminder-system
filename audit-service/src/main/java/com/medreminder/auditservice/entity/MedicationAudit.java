package com.medreminder.auditservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "medication_audits", indexes = {
        @Index(name = "idx_patient_id", columnList = "patient_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class MedicationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID auditId;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID scheduleId;

    @Column(nullable = false)
    private String medicationName;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String status;

    @Column
    private LocalDateTime scheduledTime;

    @Column
    private LocalDateTime actualTime;

    @Column
    private Long delayMinutes;

    @Column
    private String notes;

    @Column
    private String recordedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
