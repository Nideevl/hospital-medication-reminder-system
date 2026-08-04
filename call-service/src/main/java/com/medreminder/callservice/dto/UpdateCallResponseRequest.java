package com.medreminder.callservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCallResponseRequest {
    private UUID callId;
    private String callStatus;
    private String ivrResponse;
    private Integer callDurationSeconds;
}
