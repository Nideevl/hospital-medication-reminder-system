package com.medreminder.callservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallLogResponse {
    private UUID callId;
    private UUID patientId;
    private UUID scheduleId;
    private String callStatus;
    private String ivrResponse;
    private Integer callDurationSeconds;
    private LocalDateTime callInitiatedAt;
    private LocalDateTime callAnsweredAt;
    private LocalDateTime callEndedAt;
}
