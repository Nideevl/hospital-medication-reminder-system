package com.medreminder.escalationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscalationResponse {
    private UUID escalationId;
    private UUID patientId;
    private UUID scheduleId;
    private String escalationType;
    private String status;
    private LocalDateTime triggeredAt;
    private LocalDateTime notifiedAt;
}
