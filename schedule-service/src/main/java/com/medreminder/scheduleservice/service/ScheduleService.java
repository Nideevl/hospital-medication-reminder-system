package com.medreminder.scheduleservice.service;

import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.common.util.Constants;
import com.medreminder.scheduleservice.entity.Schedule;
import com.medreminder.scheduleservice.exception.ResourceNotFoundException;
import com.medreminder.scheduleservice.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduleService {
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public Schedule createSchedule(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    public Schedule getScheduleById(UUID scheduleId) {
        return scheduleRepository.findById(scheduleId).orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
    }

    public List<Schedule> getSchedulesForToday(UUID patientId) {
        return scheduleRepository.findSchedulesForToday(patientId, LocalDate.now());
    }

    public List<Schedule> getSchedulesByPatientId(UUID patientId) {
        return scheduleRepository.findByPatientId(patientId);
    }

    public Schedule updateScheduleStatus(UUID scheduleId, Boolean active) {
        Schedule schedule = getScheduleById(scheduleId);
        schedule.setActive(active);
        return scheduleRepository.save(schedule);
    }

    public void deleteSchedule(UUID scheduleId) {
        Schedule schedule = getScheduleById(scheduleId);
        scheduleRepository.delete(schedule);
    }

    public void publishMedicationDueEvent(Schedule schedule, String doseTime) {
        MedicationDueEvent event = new MedicationDueEvent();
        event.setScheduleId(schedule.getScheduleId());
        event.setPatientId(schedule.getPatientId());
        event.setMedicationId(schedule.getMedicationId());
        event.setScheduledTime(LocalTime.parse(doseTime));
        event.setLocation("in-hospital");
        event.setTimestamp(LocalDateTime.now());
        kafkaTemplate.send(Constants.KAFKA_TOPIC_MEDICATION_DUE, event);
    }
}