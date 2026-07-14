package com.medreminder.scheduleservice.repository;

import com.medreminder.scheduleservice.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    List<Schedule> findByPatientId(UUID patientId);
    List<Schedule> findByPatientIdAndActive(UUID patientId, Boolean active);
    @Query("SELECT s FROM Schedule s WHERE s.patientId = :patientId AND s.startDate <= :date AND (s.endDate IS NULL OR s.endDate >= :date) AND s.active = true")
    List<Schedule> findSchedulesForToday(@Param("patientId") UUID patientId, @Param("date") LocalDate date);
}