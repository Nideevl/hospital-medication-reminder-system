package com.medreminder.notificationservice.controller;
import com.medreminder.notificationservice.dto.NotificationResponse;
import com.medreminder.notificationservice.dto.SendNotificationRequest;
import com.medreminder.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {
    @Autowired
    private NotificationService notificationService;
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(@RequestBody SendNotificationRequest request) {
        log.info("POST /api/notifications/send - Patient: {}", request.getPatientId());
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse> getNotification(@PathVariable UUID notificationId) {
        log.info("GET /api/notifications/{}", notificationId);
        return ResponseEntity.ok(notificationService.getNotification(notificationId));
    }
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<NotificationResponse>> getPatientNotifications(@PathVariable UUID patientId) {
        log.info("GET /api/notifications/patient/{}", patientId);
        return ResponseEntity.ok(notificationService.getPatientNotifications(patientId));
    }
    @GetMapping("/pending")
    public ResponseEntity<List<NotificationResponse>> getPendingNotifications() {
        log.info("GET /api/notifications/pending");
        return ResponseEntity.ok(notificationService.getPendingNotifications());
    }
}
