package com.medreminder.patient.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_mrn", columnList = "medical_record_number"),
    @Index(name = "idx_active", columnList = "active")
})
public class Patient {

    @Id
    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20, unique = true)
    private String phoneNumber;

    @Column(name = "medical_record_number", nullable = false, unique = true, length = 50)
    private String medicalRecordNumber;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private String address;

    @Transient
    private String dateOfBirth;

    // --- ALIAS GETTERS AND SETTERS --- //

    public String getName() {
        if (firstName == null && lastName == null) return null;
        return (firstName != null ? firstName : "") + (lastName != null ? " " + lastName : "");
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) return;
        String[] parts = name.trim().split("\\s+", 2);
        this.firstName = parts[0];
        this.lastName = parts.length > 1 ? parts[1] : "";
    }

    public String getPhone() {
        return this.phoneNumber;
    }

    public void setPhone(String phone) {
        this.phoneNumber = phone;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDateOfBirth() {
        return this.dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    // --- CUSTOM BUILDER METHODS FOR TEST COMPATIBILITY --- //

    public static class PatientBuilder {
        public PatientBuilder name(String name) {
            if (name != null && !name.isBlank()) {
                String[] parts = name.trim().split("\\s+", 2);
                this.firstName = parts[0];
                this.lastName = parts.length > 1 ? parts[1] : "";
            }
            return this;
        }

        public PatientBuilder phone(String phone) {
            this.phoneNumber = phone;
            return this;
        }

        public PatientBuilder address(String address) {
            this.address = address;
            return this;
        }

        public PatientBuilder dateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }
    }
}