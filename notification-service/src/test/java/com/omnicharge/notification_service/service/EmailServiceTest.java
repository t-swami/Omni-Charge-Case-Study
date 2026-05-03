package com.omnicharge.notification_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "omnicharge8@outlook.com");
    }

    @Test
    @DisplayName("sendEmail() sends email successfully")
    void sendEmail_success() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertThatNoException().isThrownBy(
                () -> emailService.sendEmail("user@example.com", "Test Subject", "Test Body"));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail() handles mail sender exception gracefully")
    void sendEmail_exceptionHandled() {
        doThrow(new RuntimeException("SMTP connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatNoException().isThrownBy(
                () -> emailService.sendEmail("user@example.com", "Test Subject", "Test Body"));
    }

    @Test
    @DisplayName("sendEmail() does nothing when toEmail is null")
    void sendEmail_nullEmail_doesNothing() {
        assertThatNoException().isThrownBy(
                () -> emailService.sendEmail(null, "Test Subject", "Test Body"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail() does nothing when toEmail is blank")
    void sendEmail_blankEmail_doesNothing() {
        assertThatNoException().isThrownBy(
                () -> emailService.sendEmail("  ", "Test Subject", "Test Body"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
