package com.medreminder.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallResponseEvent {

    private UUID callLogId;
    private UUID scheduleId;
    private UUID patientId;
    private String ivrResponse;
    private boolean responseReceived;
    private String callStatus;
    private LocalDateTime timestamp;

    public UUID getCallLogId() {
        return callLogId;
    }

    public void setCallLogId(UUID callLogId) {
        this.callLogId = callLogId;
    }

    public UUID getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(UUID scheduleId) {
        this.scheduleId = scheduleId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getPatientIdAsString() {
        return patientId != null ? patientId.toString() : null;
    }

    public String getIvrResponse() {
        return ivrResponse;
    }

    public void setIvrResponse(String ivrResponse) {
        this.ivrResponse = ivrResponse;
    }

    public boolean isResponseReceived() {
        return responseReceived;
    }

    public void setResponseReceived(boolean responseReceived) {
        this.responseReceived = responseReceived;
    }

    public String getCallStatus() {
        return callStatus;
    }

    public void setCallStatus(String callStatus) {
        this.callStatus = callStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
