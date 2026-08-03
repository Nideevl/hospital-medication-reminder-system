package com.medreminder.auditservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordMedicationRequest {
    private UUID patientId;
    private UUID scheduleId;
    private String medicationName;
    private String action;
    private String status;
    private LocalDateTime scheduledTime;
    private LocalDateTime actualTime;
    private String notes;
}
