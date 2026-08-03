package com.medreminder.callservice.repository;

import com.medreminder.callservice.entity.CallAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CallAttemptRepository extends JpaRepository<CallAttempt, UUID> {
    List<CallAttempt> findByCallId(UUID callId);
    List<CallAttempt> findByCallIdOrderByAttemptNumber(UUID callId);
}
