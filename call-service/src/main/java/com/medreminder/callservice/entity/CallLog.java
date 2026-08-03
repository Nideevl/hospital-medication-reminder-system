package com.medreminder.callservice.entity;

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
@Table(name = "call_logs")
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID callId;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID scheduleId;

    @Column(nullable = false)
    private String callStatus;

    @Column
    private String ivrResponse;

    @Column
    private Integer callDurationSeconds;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime callInitiatedAt;

    @Column
    private LocalDateTime callAnsweredAt;

    @Column
    private LocalDateTime callEndedAt;

    @Column
    private String attemptNotes;
}
