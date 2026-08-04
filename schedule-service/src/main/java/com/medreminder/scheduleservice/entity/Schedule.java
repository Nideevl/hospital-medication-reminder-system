package com.medreminder.scheduleservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "schedules")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID scheduleId;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID medicationId;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private String doseTimes;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum Frequency { DAILY, WEEKLY, MONTHLY }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

    public LocalTime getScheduledTime() {
        if (this.doseTimes != null && !this.doseTimes.isEmpty()) {
            return LocalTime.parse(this.doseTimes.split(",")[0].trim());
        }
        return LocalTime.of(8, 0);
    }

    public static class ScheduleBuilder {
        public ScheduleBuilder scheduledTime(LocalTime time) {
            this.doseTimes = time != null ? time.toString() : null;
            return this;
        }

        public ScheduleBuilder active(Boolean active) {
            this.active$value = active;
            this.active$set = true;
            return this;
        }
    }
}