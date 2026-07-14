package com.medreminder.scheduleservice.controller;

import com.medreminder.scheduleservice.entity.Schedule;
import com.medreminder.scheduleservice.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {
    @Autowired
    private ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<Schedule> createSchedule(@RequestBody Schedule schedule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(schedule));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Schedule>> getSchedulesByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByPatientId(patientId));
    }

    @GetMapping("/patient/{patientId}/today")
    public ResponseEntity<List<Schedule>> getTodaySchedules(@PathVariable UUID patientId) {
        return ResponseEntity.ok(scheduleService.getSchedulesForToday(patientId));
    }

    @PutMapping("/{scheduleId}/status")
    public ResponseEntity<Schedule> updateStatus(@PathVariable UUID scheduleId, @RequestParam Boolean active) {
        return ResponseEntity.ok(scheduleService.updateScheduleStatus(scheduleId, active));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }
}