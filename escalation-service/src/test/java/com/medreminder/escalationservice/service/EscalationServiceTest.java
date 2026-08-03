package com.medreminder.escalationservice.service;

import com.medreminder.escalationservice.entity.Escalation;
import com.medreminder.escalationservice.exception.ResourceNotFoundException;
import com.medreminder.escalationservice.repository.EscalationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Escalation Service Unit Tests")
class EscalationServiceTest {

    @Mock
    private EscalationRepository escalationRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EscalationService escalationService;

    private Escalation testEscalation;
    private UUID escalationId;
    private UUID patientId;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        escalationId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();

        testEscalation = Escalation.builder()
                .escalationId(escalationId)
                .patientId(patientId)
                .scheduleId(scheduleId)
                .escalationType("DOSE_MISSED")
                .escalationLevel(1)
                .escalatedTo("CAREGIVER_SMS")
                .escalationTime(LocalDateTime.now())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create escalation successfully")
    void testCreateEscalation_Success() {
        // Arrange
        when(escalationRepository.save(any(Escalation.class))).thenReturn(testEscalation);

        // Act
        Escalation result = escalationService.createEscalation(testEscalation);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEscalationId()).isEqualTo(escalationId);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(escalationRepository, times(1)).save(testEscalation);
    }

    @Test
    @DisplayName("Should retrieve escalation by ID successfully")
    void testGetEscalationById_Success() {
        // Arrange
        when(escalationRepository.findById(escalationId)).thenReturn(Optional.of(testEscalation));

        // Act
        Escalation result = escalationService.getEscalationById(escalationId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEscalationId()).isEqualTo(escalationId);
        verify(escalationRepository, times(1)).findById(escalationId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when escalation not found")
    void testGetEscalationById_NotFound() {
        // Arrange
        when(escalationRepository.findById(escalationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> escalationService.getEscalationById(escalationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Escalation not found with ID: " + escalationId);
    }

    @Test
    @DisplayName("Should update escalation status successfully")
    void testUpdateEscalationStatus_Success() {
        // Arrange
        when(escalationRepository.findById(escalationId)).thenReturn(Optional.of(testEscalation));
        when(escalationRepository.save(any(Escalation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Escalation result = escalationService.updateEscalationStatus(escalationId, "IN_PROGRESS");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        verify(escalationRepository, times(1)).findById(escalationId);
        verify(escalationRepository, times(1)).save(any(Escalation.class));
    }

    @Test
    @DisplayName("Should resolve escalation successfully")
    void testResolveEscalation_Success() {
        // Arrange
        when(escalationRepository.findById(escalationId)).thenReturn(Optional.of(testEscalation));
        when(escalationRepository.save(any(Escalation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Escalation result = escalationService.resolveEscalation(escalationId, "Medication taken");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(result.getOutcome()).isEqualTo("Medication taken");
        assertThat(result.getResolvedTime()).isNotNull();
        verify(escalationRepository, times(1)).findById(escalationId);
        verify(escalationRepository, times(1)).save(any(Escalation.class));
    }

    @Test
    @DisplayName("Should publish to RabbitMQ successfully")
    void testPublishToRabbitMQ_Success() {
        // Arrange
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), any());

        // Act
        escalationService.publishEscalationToRabbitMQ(testEscalation);

        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any());
    }
}
