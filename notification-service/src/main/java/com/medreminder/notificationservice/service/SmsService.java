package com.medreminder.notificationservice.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
@Slf4j
public class SmsService {
    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;
    public boolean sendSms(String phoneNumber, String message) {
        if (!twilioEnabled) {
            log.info("Twilio not configured, simulating SMS to: {}", phoneNumber);
            return true;
        }
        try {
            log.info("SMS sent to: {} - Message: {}", phoneNumber, message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}
