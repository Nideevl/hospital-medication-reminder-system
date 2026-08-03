package com.medreminder.reportservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReportRequest {
    private UUID patientId;
    private String reportType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
