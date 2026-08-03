package com.medreminder.reportservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "compliance_data")
public class ComplianceData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID complianceId;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private LocalDate reportDate;

    @Column(nullable = false)
    private Integer medicationsScheduled;

    @Column(nullable = false)
    private Integer medicationsTaken;

    @Column(nullable = false)
    private Integer medicationsMissed;

    @Column
    private Double dailyCompliancePercentage;

    @Column
    private String notes;
}
