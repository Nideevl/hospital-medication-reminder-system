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
public class CallResponseEvent {
    private UUID scheduleId;
    private UUID callLogId;
    private String callStatus;
    private String ivrResponse;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    @Override
    public String toString() {
        return String.format("CallResponseEvent{scheduleId=%s, callLogId=%s, callStatus='%s', ivrResponse='%s', timestamp=%s}",
                scheduleId, callLogId, callStatus, ivrResponse, timestamp);
    }
}