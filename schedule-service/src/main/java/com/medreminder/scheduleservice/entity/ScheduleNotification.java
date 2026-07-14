package com.medreminder.scheduleservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "schedule_notifications")
public class ScheduleNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationId;
    @Column(nullable = false)
    private UUID scheduleId;
    @Column(nullable = false)
    private LocalDateTime notificationTime;
    @Enumerated(EnumType.STRING)
    private Status status;
    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Status { PENDING, SENT, MISSED }
}