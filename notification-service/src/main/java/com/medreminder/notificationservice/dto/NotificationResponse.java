package com.medreminder.notificationservice.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID notificationId;
    private UUID patientId;
    private String notificationType;
    private String recipient;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
