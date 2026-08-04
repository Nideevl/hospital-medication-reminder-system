package com.medreminder.scheduleservice.service;

import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.scheduleservice.entity.Schedule;
import com.medreminder.scheduleservice.exception.ResourceNotFoundException;
import com.medreminder.scheduleservice.repository.MedicationRepository;
import com.medreminder.scheduleservice.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private UUID scheduleId;
    private UUID patientId;
    private UUID medicationId;

    @BeforeEach
    void setUp() {
        scheduleId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        medicationId = UUID.randomUUID();

        testSchedule = Schedule.builder()
                .scheduleId(scheduleId)
                .patientId(patientId)
                .medicationId(medicationId)
                .startDate(LocalDate.now())
                .scheduledTime(LocalTime.of(8, 0))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create schedule successfully")
    void testCreateSchedule_Success() {
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

        Schedule result = scheduleService.createSchedule(testSchedule);

        assertThat(result).isNotNull();
        assertThat(result.getScheduleId()).isEqualTo(scheduleId);
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    @DisplayName("Should retrieve schedule by ID successfully")
    void testGetScheduleById_Success() {
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));

        Schedule result = scheduleService.getScheduleById(scheduleId);

        assertThat(result).isNotNull();
        assertThat(result.getScheduleId()).isEqualTo(scheduleId);
        assertThat(result.getPatientId()).isEqualTo(patientId);
        verify(scheduleRepository, times(1)).findById(scheduleId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when schedule not found")
    void testGetScheduleById_NotFound() {
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getScheduleById(scheduleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Schedule not found");
        verify(scheduleRepository, times(1)).findById(scheduleId);
    }

    @Test
    @DisplayName("Should retrieve schedules for today successfully")
    void testGetSchedulesForToday_Success() {
        List<Schedule> schedules = Arrays.asList(testSchedule);

        lenient().when(scheduleRepository.findSchedulesForToday(eq(patientId), any(LocalDate.class)))
                .thenReturn(schedules);

        List<Schedule> result = scheduleService.getSchedulesForToday(patientId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduleId()).isEqualTo(scheduleId);
    }

    @Test
    @DisplayName("Should retrieve all schedules for patient successfully")
    void testGetSchedulesByPatientId_Success() {
        List<Schedule> schedules = Arrays.asList(testSchedule);
        when(scheduleRepository.findByPatientId(patientId)).thenReturn(schedules);

        List<Schedule> result = scheduleService.getSchedulesByPatientId(patientId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(scheduleRepository, times(1)).findByPatientId(patientId);
    }

    @Test
    @DisplayName("Should update schedule status successfully")
    void testUpdateScheduleStatus_Success() {
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Schedule result = scheduleService.updateScheduleStatus(scheduleId, false);

        assertThat(result).isNotNull();
        assertThat(result.isActive()).isFalse();
        verify(scheduleRepository, times(1)).findById(scheduleId);
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    @DisplayName("Should delete schedule successfully")
    void testDeleteSchedule_Success() {
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
        doNothing().when(scheduleRepository).delete(testSchedule);

        scheduleService.deleteSchedule(scheduleId);

        verify(scheduleRepository, times(1)).findById(scheduleId);
        verify(scheduleRepository, times(1)).delete(testSchedule);
    }

    @Test
    @DisplayName("Should publish MedicationDueEvent to Kafka correctly")
    void testPublishMedicationDueEvent_Success() {
        lenient().when(kafkaTemplate.send(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        scheduleService.publishMedicationDueEvent(testSchedule, "08:00");

        verify(kafkaTemplate, times(1)).send(eq(Constants.KAFKA_TOPIC_MEDICATION_DUE), any(MedicationDueEvent.class));
    }
}
