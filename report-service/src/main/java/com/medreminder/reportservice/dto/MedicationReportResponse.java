package com.medreminder.reportservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReportResponse {
    private UUID reportId;
    private UUID patientId;
    private String reportType;
    private Integer totalScheduled;
    private Integer totalTaken;
    private Integer totalMissed;
    private Double compliancePercentage;
    private LocalDateTime generatedAt;
}
