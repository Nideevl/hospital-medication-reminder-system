package com.medreminder.reportservice.entity;

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
@Table(name = "medication_reports")
public class MedicationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reportId;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private String reportType;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Integer totalScheduled;

    @Column(nullable = false)
    private Integer totalTaken;

    @Column(nullable = false)
    private Integer totalMissed;

    @Column
    private Double compliancePercentage;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime generatedAt;

    @Column
    private String generatedBy;

    @Column
    private String reportStatus;
}
