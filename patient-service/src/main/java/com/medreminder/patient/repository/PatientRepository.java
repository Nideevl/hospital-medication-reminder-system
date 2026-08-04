package com.medreminder.patient.repository;

import com.medreminder.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);

    List<Patient> findByActiveTrue();

    List<Patient> findByActiveFalse();

    // Direct mappings for legacy test expectations
    Optional<Patient> findByPhoneNumber(String phoneNumber);

    default Optional<Patient> findByPhone(String phone) {
        return findByPhoneNumber(phone);
    }

    default List<Patient> findByActive(boolean active) {
        return active ? findByActiveTrue() : findByActiveFalse();
    }

    @Query("SELECT p FROM Patient p WHERE p.active = true AND (p.firstName LIKE CONCAT('%', :searchTerm, '%') OR p.lastName LIKE CONCAT('%', :searchTerm, '%'))")
    List<Patient> searchActivePatients(@Param("searchTerm") String searchTerm);
}