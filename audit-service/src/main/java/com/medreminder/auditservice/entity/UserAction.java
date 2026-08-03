package com.medreminder.auditservice.entity;

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
@Table(name = "user_actions")
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID actionId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String entity;

    @Column(nullable = false)
    private UUID entityId;

    @Column(columnDefinition = "TEXT")
    private String changeDetails;

    @Column
    private String ipAddress;

    @Column
    private String userAgent;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
