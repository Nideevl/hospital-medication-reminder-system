package com.medreminder.notificationservice.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
@Service
@Slf4j
public class EmailService {
    @Autowired(required = false)
    private JavaMailSender mailSender;
    public boolean sendEmail(String recipient, String subject, String body) {
        if (mailSender == null) {
            log.warn("JavaMailSender not configured, simulating email send");
            return true;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@medreminder.com");
            mailSender.send(message);
            log.info("Email sent to: {}", recipient);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipient, e.getMessage());
            return false;
        }
    }
}
