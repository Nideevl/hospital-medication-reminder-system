package com.medreminder.scheduleservice.service;

import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.scheduleservice.entity.Medication;
import com.medreminder.scheduleservice.entity.Schedule;
import com.medreminder.scheduleservice.exception.ResourceNotFoundException;
import com.medreminder.scheduleservice.repository.MedicationRepository;
import com.medreminder.scheduleservice.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Schedule Service Unit Tests with Kafka")
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ScheduleService scheduleService;

    private Schedule testSchedule;
    private Medication testMedication;
    private UUID scheduleId;
    private UUID patientId;
    private UUID medicationId;

    @BeforeEach
    void setUp() {
        scheduleId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        medicationId = UUID.randomUUID();

        testMedication = Medication.builder()
                .medicationId(medicationId)
                .name("Aspirin")
                .dosage("100mg")
                .frequency("Daily")
                .description("Take with food")
                .createdAt(LocalDateTime.now())
                .build();

        testSchedule = Schedule.builder()
                .scheduleId(scheduleId)
                .patientId(patientId)
                .medicationId(medicationId)
                .scheduledTime(LocalTime.of(8, 0))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create schedule and publish Kafka event")
    void testCreateSchedule_Success() throws Exception {
        // Arrange
        when(medicationRepository.findById(medicationId)).thenReturn(Optional.of(testMedication));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);
        doNothing().when(kafkaTemplate).send(any(Message.class));

        // Act
        Schedule result = scheduleService.createSchedule(testSchedule);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScheduleId()).isEqualTo(scheduleId);
        
        // Verify Kafka event was published
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate, times(1)).send(messageCaptor.capture());
        
        Message<MedicationDueEvent> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getHeaders().get("kafka_topic")).isEqualTo(Constants.KAFKA_TOPIC_MEDICATION_DUE);
        
        MedicationDueEvent event = (MedicationDueEvent) capturedMessage.getPayload();
        assertThat(event.getPatientId()).isEqualTo(patientId.toString());
        assertThat(event.getMedicationId()).isEqualTo(medicationId.toString());
        assertThat(event.getScheduledTime()).isEqualTo("08:00");
    }

    @Test
    @DisplayName("Should retrieve schedule by ID successfully")
    void testGetScheduleById_Success() {
        // Arrange
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));

        // Act
        Schedule result = scheduleService.getScheduleById(scheduleId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScheduleId()).isEqualTo(scheduleId);
        assertThat(result.getPatientId()).isEqualTo(patientId);
        verify(scheduleRepository, times(1)).findById(scheduleId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when schedule not found")
    void testGetScheduleById_NotFound() {
        // Arrange
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> scheduleService.getScheduleById(scheduleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Schedule not found with ID: " + scheduleId);
        verify(scheduleRepository, times(1)).findById(scheduleId);
    }

    @Test
    @DisplayName("Should retrieve schedules for today successfully")
    void testGetSchedulesForToday_Success() {
        // Arrange
        LocalDate today = LocalDate.now();
        List<Schedule> schedules = Arrays.asList(testSchedule);
        when(scheduleRepository.findByPatientIdAndDate(patientId, today))
                .thenReturn(schedules);

        // Act
        List<Schedule> result = scheduleService.getSchedulesForToday(patientId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduleId()).isEqualTo(scheduleId);
        verify(scheduleRepository, times(1)).findByPatientIdAndDate(patientId, today);
    }

    @Test
    @DisplayName("Should retrieve all schedules for patient successfully")
    void testGetSchedulesByPatientId_Success() {
        // Arrange
        List<Schedule> schedules = Arrays.asList(testSchedule);
        when(scheduleRepository.findByPatientId(patientId)).thenReturn(schedules);

        // Act
        List<Schedule> result = scheduleService.getSchedulesByPatientId(patientId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(scheduleRepository, times(1)).findByPatientId(patientId);
    }

    @Test
    @DisplayName("Should update schedule status successfully")
    void testUpdateScheduleStatus_Success() {
        // Arrange
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Schedule result = scheduleService.updateScheduleStatus(scheduleId, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isActive()).isFalse();
        verify(scheduleRepository, times(1)).findById(scheduleId);
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    @DisplayName("Should delete schedule successfully")
    void testDeleteSchedule_Success() {
        // Arrange
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
        doNothing().when(scheduleRepository).delete(testSchedule);

        // Act
        scheduleService.deleteSchedule(scheduleId);

        // Assert
        verify(scheduleRepository, times(1)).findById(scheduleId);
        verify(scheduleRepository, times(1)).delete(testSchedule);
    }

    @Test
    @DisplayName("Should publish MedicationDueEvent to Kafka correctly")
    void testPublishMedicationDueEvent_Success() {
        // Arrange
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        doNothing().when(kafkaTemplate).send(messageCaptor.capture());

        // Act
        scheduleService.publishMedicationDueEvent(testSchedule);

        // Assert
        verify(kafkaTemplate, times(1)).send(messageCaptor.capture());
        
        Message<MedicationDueEvent> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getHeaders().get("kafka_topic")).isEqualTo(Constants.KAFKA_TOPIC_MEDICATION_DUE);
        
        MedicationDueEvent event = capturedMessage.getPayload();
        assertThat(event.getPatientId()).isEqualTo(patientId.toString());
        assertThat(event.getMedicationId()).isEqualTo(medicationId.toString());
        assertThat(event.getScheduledTime()).isNotNull();
    }
}
