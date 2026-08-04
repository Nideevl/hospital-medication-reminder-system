package com.medreminder.notificationservice.service;

import com.medreminder.common.dto.MedicationDueEvent;
import com.medreminder.notificationservice.dto.NotificationResponse;
import com.medreminder.notificationservice.dto.SendNotificationRequest;
import com.medreminder.notificationservice.entity.Notification;
import com.medreminder.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private NotificationService notificationService;

    private UUID notificationId;
    private UUID patientId;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        notificationId = UUID.randomUUID();
        patientId = UUID.randomUUID();

        testNotification = Notification.builder()
                .notificationId(notificationId)
                .patientId(patientId)
                .notificationType("EMAIL")
                .recipient("patient@example.com")
                .subject("Medication Reminder")
                .body("Time to take your medication")
                .status("SENT")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should send email notification successfully")
    void testSendEmailNotification_Success() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .patientId(patientId)
                .notificationType("EMAIL")
                .recipient("patient@example.com")
                .subject("Medication Reminder")
                .body("Time to take your medication")
                .build();

        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationType()).isEqualTo("EMAIL");
        assertThat(response.getStatus()).isEqualTo("SENT");

        verify(emailService, times(1)).sendEmail("patient@example.com", "Medication Reminder", "Time to take your medication");
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should send SMS notification successfully")
    void testSendSmsNotification_Success() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .patientId(patientId)
                .notificationType("SMS")
                .recipient("+1234567890")
                .subject("SMS Reminder")
                .body("Take pill")
                .build();

        when(smsService.sendSms(anyString(), anyString())).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationType()).isEqualTo("SMS");
        assertThat(response.getStatus()).isEqualTo("SENT");

        verify(smsService, times(1)).sendSms("+1234567890", "Take pill");
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should mark notification FAILED when email service fails")
    void testSendEmailNotification_Failure() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .patientId(patientId)
                .notificationType("EMAIL")
                .recipient("patient@example.com")
                .subject("Medication Reminder")
                .body("Time to take your medication")
                .build();

        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should handle medication-due event from Kafka")
    void testHandleMedicationDueEvent() {
        UUID medId = UUID.randomUUID();
        MedicationDueEvent event = MedicationDueEvent.builder()
                .patientId(patientId)
                .medicationId(medId)
                .scheduledTime(java.time.LocalTime.of(8, 0))
                .build();

        when(smsService.sendSms(anyString(), anyString())).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.handleMedicationDueEvent(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should retrieve notification by ID")
    void testGetNotification_Success() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));

        NotificationResponse response = notificationService.getNotification(notificationId);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationId()).isEqualTo(notificationId);
    }

    @Test
    @DisplayName("Should throw when getting non-existent notification")
    void testGetNotification_NotFound() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> notificationService.getNotification(notificationId));
    }

    @Test
    @DisplayName("Should retrieve patient notifications")
    void testGetPatientNotifications() {
        when(notificationRepository.findByPatientId(patientId)).thenReturn(List.of(testNotification));

        List<NotificationResponse> responses = notificationService.getPatientNotifications(patientId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPatientId()).isEqualTo(patientId);
    }
}
