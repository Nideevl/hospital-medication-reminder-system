package com.medreminder.escalationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscalationRequest {
    private UUID patientId;
    private UUID scheduleId;
    private String escalationType;
    private String medicationName;
}
