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
public class DoseMissedEvent {

    private UUID scheduleId;
    private UUID patientId;
    private UUID medicationId;
    private LocalDateTime missedTime;
    private LocalDateTime timestamp;

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

    public UUID getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(UUID medicationId) {
        this.medicationId = medicationId;
    }

    public LocalDateTime getMissedTime() {
        return missedTime;
    }

    public void setMissedTime(LocalDateTime missedTime) {
        this.missedTime = missedTime;
    }

    public LocalDateTime getTimestamp() {
        return timestamp != null ? timestamp : missedTime;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
