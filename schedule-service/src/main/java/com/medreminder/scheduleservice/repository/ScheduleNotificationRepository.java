package com.medreminder.scheduleservice.repository;

import com.medreminder.scheduleservice.entity.ScheduleNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleNotificationRepository extends JpaRepository<ScheduleNotification, UUID> {
    List<ScheduleNotification> findByScheduleIdAndStatus(UUID scheduleId, ScheduleNotification.Status status);
    List<ScheduleNotification> findByStatusAndNotificationTimeBefore(ScheduleNotification.Status status, LocalDateTime time);
}