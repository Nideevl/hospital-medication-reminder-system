package com.medreminder.scheduleservice.entity;

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
@Table(name = "medications")
public class Medication {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID medicationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String dosage;

    private String frequency;

    private String description;

    private String sideEffects;

    @CreationTimestamp
    private LocalDateTime createdAt;
}