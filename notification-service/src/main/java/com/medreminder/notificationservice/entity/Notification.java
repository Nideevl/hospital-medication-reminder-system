package com.medreminder.notificationservice.entity;
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
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationId;
    @Column(nullable = false)
    private UUID patientId;
    @Column(nullable = false)
    private String notificationType;
    @Column(nullable = false)
    private String recipient;
    @Column(nullable = false)
    private String subject;
    @Column(columnDefinition = "TEXT")
    private String body;
    @Column(nullable = false)
    private String status;
    @Column
    private String externalMessageId;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @Column
    private LocalDateTime sentAt;
    @Column
    private LocalDateTime deliveredAt;
    @Column
    private String failureReason;
}
