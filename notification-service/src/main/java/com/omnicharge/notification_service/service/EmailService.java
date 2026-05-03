package com.omnicharge.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends emails via SMTP.
 * Falls back to console logging if mail sending fails.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:onmicharge@gmail.com}")
    private String fromEmail;

    /**
     * Sends a plain-text email to the specified recipient.
     * On failure, logs the error but does NOT throw — notifications must not crash.
     */
    public void sendEmail(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("  ⚠ Cannot send email — recipient address is null or blank");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            log.info("  ✓ EMAIL SENT to {} | Subject: {}", toEmail, subject);
        } catch (Exception ex) {
            log.error("  ✗ EMAIL FAILED to {} | Error: {}", toEmail, ex.getMessage());
        }
    }
}
