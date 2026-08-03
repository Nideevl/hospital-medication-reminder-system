package com.medreminder.notificationservice.service;

import com.medreminder.common.util.Constants;
import com.medreminder.notificationservice.entity.Notification;
import com.medreminder.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service Unit Tests with RabbitMQ")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private UUID notificationId;
    private UUID patientId;

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
                .message("Time to take your medication")
                .status("PENDING")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should send email notification successfully")
    void testSendEmailNotification_Success() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        doNothing().when(rabbitTemplate).convertAndSend(eq(Constants.RABBITMQ_EMAIL_QUEUE), any());

        // Act
        Notification result = notificationService.sendEmailNotification(
                patientId, "patient@example.com", "Subject", "Message body");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNotificationType()).isEqualTo("EMAIL");
        assertThat(result.getStatus()).isEqualTo("SENT");
        
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(Constants.RABBITMQ_EMAIL_QUEUE), any());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should send SMS notification successfully")
    void testSendSmsNotification_Success() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        doNothing().when(rabbitTemplate).convertAndSend(eq(Constants.RABBITMQ_SMS_QUEUE), any());

        // Act
        Notification result = notificationService.sendSmsNotification(
                patientId, "+1234567890", "SMS message");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNotificationType()).isEqualTo("SMS");
        assertThat(result.getStatus()).isEqualTo("SENT");
        
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(Constants.RABBITMQ_SMS_QUEUE), any());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should send both email and SMS notifications")
    void testSendNotification_BothChannels() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), any());

        // Act
        notificationService.sendNotification(patientId, "patient@example.com", "+1234567890", "Test message");

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(Constants.RABBITMQ_EMAIL_QUEUE), any());
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(Constants.RABBITMQ_SMS_QUEUE), any());
        verify(notificationRepository, atLeast(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should retry failed notification successfully")
    void testRetryFailedNotification_Success() {
        // Arrange
        Notification failedNotification = Notification.builder()
                .notificationId(notificationId)
                .patientId(patientId)
                .notificationType("EMAIL")
                .recipient("patient@example.com")
                .status("FAILED")
                .retryCount(2)
                .errorMessage("Connection timeout")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(failedNotification));
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(failedNotification);
        doNothing().when(rabbitTemplate).convertAndSend(eq(Constants.RABBITMQ_EMAIL_QUEUE), any());

        // Act
        Notification result = notificationService.retryNotification(notificationId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRetryCount()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(notificationRepository, times(1)).findById(notificationId);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should publish to RabbitMQ queue correctly")
    void testPublishToRabbitMQ_Success() {
        // Arrange
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        doNothing().when(rabbitTemplate).convertAndSend(eq(Constants.RABBITMQ_EMAIL_QUEUE), messageCaptor.capture());

        // Act
        notificationService.publishToRabbitMQ(testNotification, Constants.RABBITMQ_EMAIL_QUEUE);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(Constants.RABBITMQ_EMAIL_QUEUE), any());
        
        Object sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).isNotNull();
    }

    @Test
    @DisplayName("Should handle RabbitMQ publish failure gracefully")
    void testPublishToRabbitMQ_Failure() {
        // Arrange
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(eq(Constants.RABBITMQ_EMAIL_QUEUE), any());

        // Act
        notificationService.publishToRabbitMQ(testNotification, Constants.RABBITMQ_EMAIL_QUEUE);

        // Assert - Should not throw, status should be FAILED
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}
