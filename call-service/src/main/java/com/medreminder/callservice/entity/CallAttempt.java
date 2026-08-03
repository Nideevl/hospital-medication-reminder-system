package com.medreminder.callservice.entity;

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
@Table(name = "call_attempts")
public class CallAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID attemptId;

    @Column(nullable = false)
    private UUID callId;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime attemptedAt;

    @Column
    private LocalDateTime respondedAt;

    @Column
    private String responseCode;
}
