package com.medreminder.common.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoseMissedEvent {
    private UUID scheduleId;
    private UUID patientId;
    private UUID medicationId;
    private String reason;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    @Override
    public String toString() {
        return String.format("DoseMissedEvent{scheduleId=%s, patientId=%s, medicationId=%s, reason='%s', timestamp=%s}",
                scheduleId, patientId, medicationId, reason, timestamp);
    }
}