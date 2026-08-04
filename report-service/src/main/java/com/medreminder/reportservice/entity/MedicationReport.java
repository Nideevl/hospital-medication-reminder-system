package com.medreminder.reportservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medication_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private UUID reportId;
    private UUID patientId;
    private String reportType;
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;

    private int totalMedicationsScheduled;
    private int totalScheduled;
    private int medicationsTaken;
    private int totalTaken;
    private int medicationsMissed;
    private int totalMissed;

    private double adherencePercentage;
    private BigDecimal compliancePercentage;

    private String reportStatus;
    private String generatedBy;
    private String summary;

    private LocalDateTime createdAt;
    private LocalDateTime generatedAt;

    public void setStartDate(LocalDateTime dateTime) {
        this.startDate = dateTime != null ? dateTime.toLocalDate() : null;
    }

    public void setStartDate(LocalDate date) {
        this.startDate = date;
    }

    public void setEndDate(LocalDateTime dateTime) {
        this.endDate = dateTime != null ? dateTime.toLocalDate() : null;
    }

    public void setEndDate(LocalDate date) {
        this.endDate = date;
    }

    public Double getCompliancePercentage() {
        if (compliancePercentage != null) {
            return compliancePercentage.doubleValue();
        }
        return adherencePercentage;
    }

    public void setCompliancePercentage(double value) {
        this.compliancePercentage = BigDecimal.valueOf(value);
        this.adherencePercentage = value;
    }

    public void setCompliancePercentage(BigDecimal value) {
        this.compliancePercentage = value;
        if (value != null) {
            this.adherencePercentage = value.doubleValue();
        }
    }
}
