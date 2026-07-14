package com.medreminder.scheduleservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "schedules")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID scheduleId;
    @Column(nullable = false)
    private UUID patientId;
    @Column(nullable = false)
    private UUID medicationId;
    @Column(nullable = false)
    private LocalDate startDate;
    private LocalDate endDate;
    @Column(nullable = false)
    private String doseTimes;
    @Enumerated(EnumType.STRING)
    private Frequency frequency;
    @Column(nullable = false)
    private Boolean active = true;
    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Frequency { DAILY, WEEKLY, MONTHLY }
}