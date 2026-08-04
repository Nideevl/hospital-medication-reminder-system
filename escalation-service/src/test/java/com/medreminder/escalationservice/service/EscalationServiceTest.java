package com.medreminder.escalationservice.service;

import com.medreminder.escalationservice.dto.EscalationRequest;
import com.medreminder.escalationservice.dto.EscalationResponse;
import com.medreminder.escalationservice.entity.Escalation;
import com.medreminder.escalationservice.exception.EscalationException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        testEscalation = new Escalation();
        testEscalation.setEscalationId(escalationId);
        testEscalation.setPatientId(patientId);
        testEscalation.setScheduleId(scheduleId);
        testEscalation.setEscalationType("MISSED_DOSE");
        testEscalation.setEscalationLevel(1);
        testEscalation.setEscalatedTo("CAREGIVER_SMS");
        testEscalation.setEscalationTime(LocalDateTime.now());
        testEscalation.setStatus("TRIGGERED");
        testEscalation.setCreatedAt(LocalDateTime.now());
        testEscalation.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should trigger escalation successfully")
    void testTriggerEscalation_Success() {
        // Arrange
        EscalationRequest request = new EscalationRequest();
        request.setPatientId(patientId);
        request.setScheduleId(scheduleId);
        request.setEscalationType("MISSED_DOSE");
        request.setMedicationName("Aspirin");

        when(escalationRepository.save(any(Escalation.class))).thenReturn(testEscalation);

        // Act
        EscalationResponse response = escalationService.triggerEscalation(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEscalationId()).isEqualTo(escalationId);
        verify(escalationRepository, times(1)).save(any(Escalation.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("med-reminder-exchange"), eq("escalation.route"), any(Escalation.class));
    }

    @Test
    @DisplayName("Should retrieve escalation by ID successfully")
    void testGetEscalation_Success() {
        // Arrange
        when(escalationRepository.findById(escalationId)).thenReturn(Optional.of(testEscalation));

        // Act
        EscalationResponse response = escalationService.getEscalation(escalationId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEscalationId()).isEqualTo(escalationId);
        verify(escalationRepository, times(1)).findById(escalationId);
    }

    @Test
    @DisplayName("Should throw EscalationException when escalation not found")
    void testGetEscalation_NotFound() {
        // Arrange
        when(escalationRepository.findById(escalationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> escalationService.getEscalation(escalationId))
                .isInstanceOf(EscalationException.class)
                .hasMessageContaining("Escalation not found");
    }

    @Test
    @DisplayName("Should resolve escalation successfully")
    void testResolveEscalation_Success() {
        // Arrange
        when(escalationRepository.findById(escalationId)).thenReturn(Optional.of(testEscalation));
        when(escalationRepository.save(any(Escalation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        EscalationResponse response = escalationService.resolveEscalation(escalationId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(escalationRepository, times(1)).findById(escalationId);
        verify(escalationRepository, times(1)).save(any(Escalation.class));
    }
}
