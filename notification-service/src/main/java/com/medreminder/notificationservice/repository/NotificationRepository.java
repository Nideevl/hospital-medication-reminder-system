package com.medreminder.notificationservice.repository;

import com.medreminder.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByPatientId(UUID patientId);

    Optional<Notification> findFirstByPatientIdOrderByCreatedAtDesc(UUID patientId);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' ORDER BY n.createdAt ASC")
    List<Notification> findPendingNotifications();

    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.createdAt >= ?1")
    List<Notification> findFailedNotificationsAfter(LocalDateTime fromDateTime);
}
