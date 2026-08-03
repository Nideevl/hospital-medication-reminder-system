package com.medreminder.auditservice.repository;

import com.medreminder.auditservice.entity.UserAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, UUID> {

    List<UserAction> findByUserIdOrderByTimestampDesc(UUID userId);

    @Query("SELECT ua FROM UserAction ua WHERE ua.timestamp >= ?1 AND ua.timestamp <= ?2 ORDER BY ua.timestamp DESC")
    List<UserAction> findActionsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT ua FROM UserAction ua WHERE ua.entity = ?1 AND ua.entityId = ?2 ORDER BY ua.timestamp DESC")
    List<UserAction> findActionsByEntity(String entity, UUID entityId);
}
