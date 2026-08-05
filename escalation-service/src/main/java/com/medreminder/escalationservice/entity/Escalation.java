package com.medreminder.escalationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "escalations")
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "escalation_id", nullable = false, updatable = false)
    private UUID escalationId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "escalation_type", nullable = false)
    private String escalationType;

    @Column(name = "escalation_level", nullable = false)
    private int escalationLevel = 1;

    @Column(name = "escalated_to")
    private String escalatedTo;

    @Column(name = "caregiver_phone_number")
    private String caregiverPhoneNumber;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "escalation_notes")
    private String escalationNotes;

    @CreationTimestamp
    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "escalation_time")
    private LocalDateTime escalationTime;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}