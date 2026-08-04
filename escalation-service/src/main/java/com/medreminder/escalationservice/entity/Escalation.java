package com.medreminder.escalationservice.entity;

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
@Table(name = "escalations")
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID escalationId;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID scheduleId;

    @Column(nullable = false)
    private String escalationType;

    @Column
    private String caregiverPhoneNumber;

    @Column(nullable = false)
    private String status;

    @Column
    private String escalationNotes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime triggeredAt;

    @Column
    private LocalDateTime notifiedAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column
    private int escalationLevel;

    @Column
    private String escalatedTo;

    @Column
    private LocalDateTime escalationTime;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

}
