package com.medreminder.reportservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "compliance_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private UUID patientId;
    private int weekNumber;
    @Column(name = "\"month\"")
    private int month;
    private double adherenceScore;
    private int missedDoses;
    private int escalationsTriggered;
    private LocalDateTime createdAt;

    public void setAdherenceScore(BigDecimal value) {
        if (value != null) {
            this.adherenceScore = value.doubleValue();
        }
    }

    public void setAdherenceScore(double value) {
        this.adherenceScore = value;
    }

    public void setWeekNumber(String week) {
        if (week != null) {
            try {
                this.weekNumber = Integer.parseInt(week);
            } catch (NumberFormatException e) {
                this.weekNumber = 0;
            }
        }
    }

    public void setWeekNumber(int week) {
        this.weekNumber = week;
    }
}
