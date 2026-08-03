package com.medreminder.auditservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActionDTO {
    private UUID actionId;
    private UUID userId;
    private String actionType;
    private String entity;
    private UUID entityId;
    private String changeDetails;
    private LocalDateTime timestamp;
}
