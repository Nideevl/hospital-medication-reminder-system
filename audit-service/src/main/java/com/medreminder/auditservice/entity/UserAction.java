package com.medreminder.auditservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_actions", indexes = {
        @Index(name = "idx_user_actions_user_id", columnList = "user_id"),
        @Index(name = "idx_user_actions_action_type", columnList = "action_type")
})
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "action_id", nullable = false, updatable = false)
    private UUID actionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "entity", nullable = false)
    private String entity;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "change_details", columnDefinition = "TEXT")
    private String changeDetails;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}