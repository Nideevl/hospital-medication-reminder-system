package com.medreminder.callservice.entity;

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
@Table(name = "call_logs")
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "call_id", nullable = false, updatable = false)
    private UUID callId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "call_status", nullable = false)
    private String callStatus;

    @Column(name = "ivr_response")
    private String ivrResponse;

    @Column(name = "call_duration_seconds")
    private Integer callDurationSeconds;

    @Column(name = "response_received", nullable = false)
    private Boolean responseReceived = false;

    @Column(name = "attempt_notes")
    private String attemptNotes;

    @CreationTimestamp
    @Column(name = "call_initiated_at", nullable = false, updatable = false)
    private LocalDateTime callInitiatedAt;

    @Column(name = "call_answered_at")
    private LocalDateTime callAnsweredAt;

    @Column(name = "call_ended_at")
    private LocalDateTime callEndedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}